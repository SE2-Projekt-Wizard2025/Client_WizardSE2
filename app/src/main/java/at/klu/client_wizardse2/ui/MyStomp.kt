package at.klu.client_wizardse2.ui
import android.os.Handler
import android.os.Looper
import android.util.Log
import at.klu.client_wizardse2.ui.Callbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import org.json.JSONObject

private  val WEBSOCKET_URI = "ws://10.0.2.2:8080/websocket-example-broker";
class MyStomp(val callbacks: Callbacks) {

    private lateinit var topicFlow: Flow<String>
    private lateinit var collector:Job

    private lateinit var jsonFlow: Flow<String>
    private lateinit var jsonCollector:Job

    private lateinit var client:StompClient
    private lateinit var session: StompSession

    private val scope:CoroutineScope=CoroutineScope(Dispatchers.IO)
    fun connect() {

            client = StompClient(OkHttpWebSocketClient()) // other config can be passed in here
            scope.launch {
                session=client.connect(WEBSOCKET_URI)
                topicFlow= session.subscribeText("/topic/hello-response")
                //connect to topic
                collector=scope.launch { topicFlow.collect{
                        msg->
                    //todo logic
                    callback(msg)
                } }

                //connect to topic
                jsonFlow= session.subscribeText("/topic/rcv-object")
                jsonCollector=scope.launch { jsonFlow.collect{
                        msg->
                    var o=JSONObject(msg)
                    callback(o.get("text").toString())
                } }
                callback("connected")
            }

    }
    private fun callback(msg:String){
        Handler(Looper.getMainLooper()).post{
            callbacks.onResponse(msg)
        }
    }
    fun sendHello(){

        scope.launch {
            Log.e("tag","connecting to topic")

            session.sendText("/app/hello","message from client")
           }
    }
    fun sendJson(){
        var json=JSONObject();
        json.put("from","client")
        json.put("text","from client")
        var o=json.toString()

        scope.launch {
            session.sendText("/app/object",o);
        }

    }

}