package jp.techacademy.takanari.qa_app
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import android.util.Base64  //追加する
import android.widget.ListView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class FavoriteActivity : AppCompatActivity(){
    private var mGenre = 0
    private lateinit var mQuestion: Question
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView1: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private lateinit var mFavoriteRef:DatabaseReference
//    private lateinit var mToolbar: Toolbar



    var favuid = ""

    private var mGenreRef: DatabaseReference? = null
    private val mFavoriteListener = object : ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {

        }

        override fun onChildRemoved(p0: DataSnapshot) {
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, p1: String?) {
            val map = dataSnapshot.value as Map<String, String>
            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }

        }


        override fun onChildAdded(dataSnapshot: DataSnapshot, p1: String?) {
            mFavoriteRef = mDatabaseReference.child(FavoritePath).child(favuid)
            mFavoriteRef.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(dataSnapshot2: DataSnapshot) {
                    //qidがdatasnapshotと同じ時＝取得してくるデータ(お気に入りされているデータ)
                    if (dataSnapshot2.hasChild(dataSnapshot.key.toString())) {
                        //boolean型(押されたか)
                        val map = dataSnapshot.value as Map<String, String>
                        val title = map["title"] ?: ""
                        val body = map["body"] ?: ""
                        val name = map["name"] ?: ""
                        val uid = map["uid"] ?: ""
                        val imageString = map["image"] ?: ""
                        val bytes =
                            if (imageString.isNotEmpty()) {
                                Base64.decode(imageString, Base64.DEFAULT)
                            } else {
                                byteArrayOf()
                            }
                        val answerArrayList = ArrayList<Answer>()
                        val answerMap = map["answers"] as Map<String, String>?
                        if (answerMap != null) {
                            for (key in answerMap.keys) {
                                val temp = answerMap[key] as Map<String, String>
                                val answerBody = temp["body"] ?: ""
                                val answerName = temp["name"] ?: ""
                                val answerUid = temp["uid"] ?: ""
                                val answer = Answer(answerBody, answerName, answerUid, key)
                                answerArrayList.add(answer)
                            }
                        }
                        //Fierebaseから撮ってきたデータをリストにしているだけ
                        val question = Question(
                            title, body, name, uid, dataSnapshot.key ?: "",
                            mGenre, bytes, answerArrayList
                        )
                        mQuestionArrayList.add(question)
                        mAdapter.notifyDataSetChanged()
                    }

                }
            })
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        val user = FirebaseAuth.getInstance().currentUser
        //userがログインしている時
        if (user != null) {
            //ログインされてたらuidをセットする
            favuid = user.uid
        }

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference
        // ListViewの準備
        mListView1 = findViewById(R.id.listView1)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()
        //ListViewのsetOnItemClickListenerメソッドでリスナーを登録し、
        // リスナーの中で質問に相当するQuestionのインスタンスを渡してQuestionDetailActivityに遷移させる
        mListView1.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            //applicationContextはアプリケーションの情報を持っている(高さ、幅とか)を渡している
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
        // 選択したジャンルにリスナーを登録する
//        if (mGenreRef != null) {
//            mGenreRef!!.removeEventListener(mFavoriteListener)
//        }
    }

    override fun onResume() {
        super.onResume()
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        mListView1.adapter = mAdapter
        for (mGenre1 in 1..4) {
            mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre1.toString())
            mGenreRef!!.addChildEventListener(mFavoriteListener)
        }

    }
}