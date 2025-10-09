package com.example.kirikata_sensei_android.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class StudentSocketManager(
    private val token: String,
    private val username: String
) {
    lateinit var socket: Socket
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    fun connect(
        onConnected: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                reconnection = true
            }

            socket = IO.socket("http://bitter1326.mydns.jp:3000", opts)
            socket.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "✅ 生徒としてSocket接続完了")
                mainHandler.post { onConnected() }
            }

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val err = args.firstOrNull()?.toString() ?: "不明なエラー"
                Log.e("SocketIO", "❌ 接続エラー: $err")
                mainHandler.post { onError(err) }
            }

            socket.connect()
        } catch (e: Exception) {
            Log.e("SocketIO", "❌ Exception: ${e.message}")
            mainHandler.post { onError(e.message ?: "不明な例外") }
        }
    }

    fun joinRoom(
        roomName: String,
        seatIndex: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!::socket.isInitialized) {
            mainHandler.post { onError("Socket未初期化です") }
            return
        }

        val data = JSONObject().apply {
            put("role", "student")
            put("room", roomName)
            put("username", username)
            put("seatIndex", seatIndex)
        }

        socket.emit("join", data)
        Log.d("SocketIO", "📤 join送信: room=$roomName seat=$seatIndex")

        socket.on("message") { args ->
            val msg = args.firstOrNull()?.toString() ?: return@on
            val dataObj = JSONObject(msg)
            val type = dataObj.optString("type")

            when (type) {
                "student_joined" -> {
                    Log.d("SocketIO", "👋 ${dataObj.optString("username")} が参加しました")
                    mainHandler.post { onSuccess() }
                }
                "room_not_found" -> mainHandler.post { onError("ルームが存在しません。先生が開始するまでお待ちください。") }
                "room_full" -> mainHandler.post { onError("ルームが満室です。") }
                "seat_occupied" -> mainHandler.post { onError("座席はすでに使用中です。") }
                "invalid_seat" -> mainHandler.post { onError("無効な座席番号です。") }
                "error" -> {
                    val msgText = dataObj.optString("message", "不明なエラー")
                    mainHandler.post { onError(msgText) }
                }
            }
        }
    }

    /** 調理開始通知（先生側の sessionStarted イベント） */
    fun onSessionStarted(onStart: (String) -> Unit) {
        socket.on("sessionStarted") { args ->
            val data = args.firstOrNull() as? JSONObject
            val recipeId = data?.optString("recipeId") ?: "unknown"
            Log.d("SocketIO", "🍳 sessionStarted 受信: $recipeId")
            mainHandler.post { onStart(recipeId) }
        }
    }

    /** セッション終了通知（先生から endSession イベント） */
    fun onSessionEnded(onEnded: () -> Unit) {
        if (!::socket.isInitialized) {
            Log.w("SocketIO", "⚠ onSessionEnded呼び出し時にソケット未初期化")
            return
        }

        socket.on("sessionEnded") { _ ->
            Log.d("SocketIO", "📩 セッション終了通知を受信しました")
            mainHandler.post { onEnded() }
        }
    }

    /** 進捗報告: studentProgress イベント送信 */
    fun emitProgress(
        room: String,
        userId: String,
        username: String,
        seatIndex: Int,
        currentStep: Int,
        recipeId: String
    ) {
        if (!::socket.isInitialized || !socket.connected()) {
            Log.w("SocketIO", "⚠ emitProgress呼び出し時にソケット未接続")
            return
        }

        val data = JSONObject().apply {
            put("room", room)
            put("userId", userId)
            put("username", username)
            put("seatIndex", seatIndex)
            put("currentStep", currentStep)
            put("recipeId", recipeId)
        }

        socket.emit("studentProgress", data)
        Log.d("SocketIO", "📤 studentProgress送信: $data")
    }

    /** 切断処理 */
    fun disconnect() {
        if (::socket.isInitialized && socket.connected()) {
            socket.disconnect()
            Log.d("SocketIO", "🛑 Socket切断")
        }
    }
}
