package jp.techacademy.takanari.qa_app

//onCreateメソッドでは渡ってきたQuestionクラスのインスタンスを保持し、タイトルを設定します。そして、ListViewの準備をします。
//FABをタップしたらログインしていなければログイン画面に遷移させ、ログインしていれば後ほど作成する回答作成画面に遷移させる準備をしておきます。
// そして重要なのがFirebaseへのリスナーの登録です。回答作成画面から戻ってきた時にその回答を表示させるために登録しておきます。

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap



class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef:DatabaseReference
    private lateinit var mUseRef:DatabaseReference
    val dataBaseReference = FirebaseDatabase.getInstance().reference

    var favuid = ""

    //isfavoriteがtrueはお気に入りされているかのフラグ
    var isfavorite=false



    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""


            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }


    //お気に入りボタンを押された時
    private val mFavoriteListener = object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            //qidがdatasnapshotと同じ時＝取得してくるデータ(お気に入りされているデータ)
            if (dataSnapshot.hasChild(mQuestion.questionUid)){
                //boolean型(押されたか)

                ivIcon1.setImageResource(android.R.drawable.star_big_on)
                isfavorite=true
            }else{

                ivIcon1.setImageResource(android.R.drawable.star_big_off)
                isfavorite=false
            }
        }
        override fun onCancelled(databaseError: DatabaseError) {

        }
    }


    //お気に入りボタンが押された時
    private val mOnIcon1ClickLitener = View.OnClickListener{
        //isfavoriteがtrueはお気に入りされているかのフラグ
        isfavorite =! isfavorite
        //favorite分岐、favid(uidと似たもの)、(詳細質問画面のidのqid)を取得
        mUseRef = dataBaseReference.child(FavoritePath).child(favuid).child(mQuestion.questionUid)
        if (isfavorite){

            //箱用意
            val data = HashMap<String, String>()

            //mQuestion.questionUid=qid
            data["genre"] = mQuestion.genre.toString()

            //FireBaseにお気に入りデータを入れる(qidとジャンル分岐)
            mUseRef.setValue(data)
        }else {
            //FireBaseのお気に入りデータを削除
            mUseRef.removeValue()
        }
    }



    //onCreateメソッドでは渡ってきたQuestionクラスのインスタンスを保持し、
    // タイトルを設定します。そして、ListViewの準備をします。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        ivIcon1.setOnClickListener(mOnIcon1ClickLitener)

        val user = FirebaseAuth.getInstance().currentUser

        //userがログインしている時
        if (user != null) {
            //ログインされてたらuidをセットする
            favuid = user.uid
        }


        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        //FABをタップしたらログインしていなければログイン画面に遷移させ、
        // ログインしていれば後ほど作成する回答作成画面に遷移させる準備をしておきます。
        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する

                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        //Answerの中身をとるための準備
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)


        //uid以下のとこを見てる　(favorite分岐、favidを追加準備)
        mFavoriteRef = dataBaseReference.child(FavoritePath).child(favuid)
        mFavoriteRef.addValueEventListener(mFavoriteListener)
    }
}