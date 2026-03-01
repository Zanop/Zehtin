package com.zehtin.app

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import android.content.Context
import java.util.UUID

object WebSocketManager {
    private const val SERVER_URL = "wss://torbalan.ddns.net/zehtin"
    private const val TAG = "ZehtinWS"

    private var appContext: Context? = null
    private var pingJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    var myId: String = ""
    var myName: String = ""

    var savedName: String = ""
    var savedInviteCode: String = ""
    private var persistentId: String = ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _memberCount = MutableStateFlow(0)
    val memberCount: StateFlow<Int> = _memberCount

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    private var lastInviteCode: String = ""

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Joined : ConnectionState()
        object Disconnected : ConnectionState()
        object Error : ConnectionState()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences("zehtin", Context.MODE_PRIVATE)
        persistentId = prefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
        savedName = prefs.getString("name", "") ?: ""
        savedInviteCode = prefs.getString("invite_code", "") ?: ""
    }

    fun saveCredentials(context: Context, name: String, inviteCode: String) {
        val prefs = context.getSharedPreferences("zehtin", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("name", name)
            .putString("invite_code", inviteCode)
            .apply()
        savedName = name
        savedInviteCode = inviteCode
    }


    fun connect(name: String, inviteCode: String) {
        myName = name
        lastInviteCode = inviteCode
        client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(SERVER_URL).build()
        webSocket = client!!.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Zehtin server")
                _connectionState.value = ConnectionState.Connected
                webSocket.send(JSONObject().apply {
                    put("type", "join")
                    put("name", name)
                    put("inviteCode", inviteCode)
                    put("deviceId", persistentId)
                }.toString())

                // Start keepalive ping every 25 seconds
                pingJob?.cancel()
                pingJob = scope.launch {
                    while (true) {
                        kotlinx.coroutines.delay(25000)
                        webSocket.send(JSONObject().apply {
                            put("type", "ping")
                        }.toString())
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {

                        "joined" -> {
                            myId = json.getString("id")
                            _members.value = emptyList()

                            // Load existing members
                            val membersList = json.optJSONArray("members")
                            if (membersList != null) {
                                for (i in 0 until membersList.length()) {
                                    val m = membersList.getJSONObject(i)
                                    updateMembers(m.getString("id"), m.getString("name"), true)
                                }
                            }
                            // Add yourself if not already in list
                            updateMembers(myId, myName, true)

                            // Load message history
                            val history = json.getJSONArray("history")
                            val msgs = mutableListOf<Message>()
                            for (i in 0 until history.length()) {
                                msgs.add(parseMessage(history.getJSONObject(i)))
                            }
                            _messages.value = msgs
                            _connectionState.value = ConnectionState.Joined
                        }

                        "member_joined" -> {
                            val name = json.getString("name")
                            val id = json.optString("memberId", name)
                            updateMembers(id, name, true)
                            _memberCount.value = json.getInt("memberCount")
                        }

                        "member_left" -> {
                            val name = json.getString("name")
                            removeMembers(name)
                            _memberCount.value = json.getInt("memberCount")
                        }

                        "member_renamed" -> {
                            val deviceId = json.getString("deviceId")
                            val newName = json.getString("newName")
                            val current = _members.value.toMutableList()
                            val index = current.indexOfFirst { it.id == deviceId }
                            if (index != -1) {
                                val initials = newName.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                current[index] = current[index].copy(name = newName, initials = initials)
                                _members.value = current
                            }
                        }

                        "message" -> {
                            val msg = parseMessage(json.getJSONObject("message"))
                            _messages.value = _messages.value + msg
                        }

                        "error" -> {
                            Log.e(TAG, "Server error: ${json.getString("text")}")
                            _connectionState.value = ConnectionState.Error
                            pingJob?.cancel()
                            myName = ""
                            lastInviteCode = ""
                            _members.value = emptyList()
                            _messages.value = emptyList()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error: $e")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failed: $t")
                _connectionState.value = ConnectionState.Error
                pingJob?.cancel()
                // Auto reconnect after 3 seconds
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (myName.isNotEmpty()) {
                        Log.d(TAG, "Attempting reconnect...")
                        connect(myName, lastInviteCode)
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
                pingJob?.cancel()
            }
        })
    }

    fun sendMessage(text: String) {
        webSocket?.send(JSONObject().apply {
            put("type", "message")
            put("text", text)
            put("deviceId", persistentId)
        }.toString())
    }

    fun sendMedia(mediaName: String, mediaSize: String) {
        webSocket?.send(JSONObject().apply {
            put("type", "media")
            put("mediaName", mediaName)
            put("mediaSize", mediaSize)
            put("deviceId", persistentId)
        }.toString())
    }

    private fun updateMembers(id: String, name: String, isOnline: Boolean) {
        val initials = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
        val current = _members.value.toMutableList()
        if (current.none { it.id == id }) {
            current.add(Member(id = id, name = name, initials = initials, isOnline = isOnline))
            _members.value = current
        }
    }

    private fun removeMembers(name: String) {
        _members.value = _members.value.filter { it.name != name }
    }

    fun disconnect() {
        pingJob?.cancel()
        webSocket?.close(1000, "User left")
        client?.dispatcher?.executorService?.shutdown()
    }

    private fun parseMessage(json: JSONObject): Message {
        return Message(
            id = json.optString("id"),
            senderId = json.optString("senderId"),
            senderName = json.optString("senderName"),
            text = json.optString("text"),
            time = json.optString("time"),
            isOutgoing = json.optString("senderId") == myId,
            isMedia = json.optBoolean("isMedia", false),
            mediaName = json.optString("mediaName"),
            mediaSize = json.optString("mediaSize")
        )
    }

    fun updateMemberName(newName: String) {
        myName = newName
        savedName = newName
        // Save silently to prefs
        appContext?.getSharedPreferences("zehtin", Context.MODE_PRIVATE)
            ?.edit()?.putString("name", newName)?.apply()

        val current = _members.value.toMutableList()
        val index = current.indexOfFirst { it.id == myId }
        if (index != -1) {
            val old = current[index]
            val initials = newName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
            current[index] = old.copy(name = newName, initials = initials)
            _members.value = current
        }
        webSocket?.send(JSONObject().apply {
            put("type", "rename")
            put("newName", newName)
            put("deviceId", persistentId)
        }.toString())
    }

}