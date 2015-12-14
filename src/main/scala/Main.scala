package org.pii5656.morphologicalanalysis
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import org.atilika.kuromoji.Tokenizer
import org.atilika.kuromoji.Token
import scala.collection.JavaConversions._
import twitter4j._
import twitter4j.conf._

import org.pii5656.morphologicalanalysis.DB._
import org.pii5656.morphologicalanalysis.Account._

object TweetResearch {
  def getMyTimeline(): ResponseList[Status] = {

    val cb = new ConfigurationBuilder
    cb.setOAuthConsumerKey(ConKey)
      .setOAuthConsumerSecret(ConSec)
      .setOAuthAccessToken(AccKey)
      .setOAuthAccessTokenSecret(AccSec)

    val twitterFactory = new TwitterFactory(cb.build)
    val twitter = twitterFactory.getInstance

    twitter.getHomeTimeline // statuses
  }

  def tweetAnalize(st: Status): List[Word] = {
    val tokenizer = Tokenizer.builder.mode(Tokenizer.Mode.NORMAL).build
    val tokens = tokenizer.tokenize(st.getText.replace("\n","")).toList

    for {
      t <- tokens
    } yield {
      val token = t.asInstanceOf[Token]
      Word(token.getSurfaceForm, token.getPartOfSpeech, 1, st.getId)
    }
  }

  def Main(): Unit = {
    // データベース(テーブル)作成
    //Await.result(DB.createTables(DB.db), Duration.Inf)

    while(true) {
     // 自分のタイムライン取得
     val statuses = getMyTimeline()
     // 各テキストを解析してリストでまとめる
     // val wordslist = statuses.map(st => tweetAnalize(st)).toList
     val wordslist = for (st <- statuses) yield {
       if(Await.result(DB.existSameTweet(st.getId), Duration.Inf))
         List()
       else
         tweetAnalize(st)
     }.toList
     // 結果をデータベースに格納
     val futureCount = for {
       words <- wordslist
       word <-words
     } yield {
       val count = Await.result(DB.getSameWordCount(word), Duration.Inf).getOrElse(-1)
       if(count <= 0) {
         Await.result(DB.insertWord(word), Duration.Inf)
       } else {
         Await.result(DB.updateWordCount(word, count + 1), Duration.Inf)
       }
     }

     println("completed")
     Thread.sleep(60000) // 1min => 60000
    }
  }
}
