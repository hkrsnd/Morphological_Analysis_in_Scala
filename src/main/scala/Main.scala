package org.pii5656.morphologicalanalysis
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Random
import org.atilika.kuromoji.Tokenizer
import org.atilika.kuromoji.Token
import scala.collection.JavaConversions._
import twitter4j._
import twitter4j.conf._

import org.pii5656.morphologicalanalysis.DB._
import org.pii5656.morphologicalanalysis.Account._

object TweetResearch {
    val cb = new ConfigurationBuilder
    cb.setOAuthConsumerKey(ConKey)
      .setOAuthConsumerSecret(ConSec)
      .setOAuthAccessToken(AccKey)
      .setOAuthAccessTokenSecret(AccSec)

    val twitterFactory = new TwitterFactory(cb.build)
    val twitter = twitterFactory.getInstance
  def getMyTimeline(): ResponseList[Status] = {


    twitter.getHomeTimeline // statuses
  }

  def tweet() =
    twitter.updateStatus(GenerateSentence.generateSentence())

  def tweetAnalize(st: Status): List[Word] = {
    //Regex pattern
    val invalidpattern = "^.*[^\\x01-\\x7E].*" //日本語(４ビット)以外の文字を含むパターン
    val tokenizer = Tokenizer.builder.mode(Tokenizer.Mode.NORMAL).build
    val tokens = tokenizer.tokenize(st.getText.replace("\n","")).toList.filter( token =>
        token.getSurfaceForm.matches(invalidpattern))
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
     tweet()

     Thread.sleep(60000) // 1min => 60000
    }
  }
}

object GenerateSentence {
  // Regex Pattern
  val nounpattern = "^名詞.*"
  val verbpattern = "^動詞.*"
  val adjectivepattern = "^形容詞.*"
  val adverbpattern = "^副詞.*"

  val allwords = Await.result(getAllWords(), Duration.Inf).toList
  val noun_list = allwords.filter(_.pofspeech.matches(nounpattern))
  val verb_list = allwords.filter(_.pofspeech.matches(verbpattern))
  val adjective_list = allwords.filter(_.pofspeech.matches(adjectivepattern))
  val adverb_list = allwords.filter(_.pofspeech.matches(adverbpattern))
  val kakujoshi_list = List("が", "の", "を", "に", "へ", "と", "から", "より", "で", "や")
  val r = new Random

  def generateSentence(): String = {
    val sentence = r.nextInt(3) match {
      case 0 => 
        noun_list(r.nextInt(noun_list.length)).word + adjective_list(r.nextInt(adjective_list.length)).word + kakujoshi_list(r.nextInt(kakujoshi_list.length)) + noun_list(r.nextInt(noun_list.length)).word + adjective_list(r.nextInt(adjective_list.length)).word + noun_list(r.nextInt(noun_list.length)).word
      case 1 => 
        noun_list(r.nextInt(noun_list.length)).word + adjective_list(r.nextInt(adjective_list.length)).word + noun_list(r.nextInt(noun_list.length)).word + kakujoshi_list(r.nextInt(kakujoshi_list.length)) + noun_list(r.nextInt(noun_list.length)).word + verb_list(r.nextInt(verb_list.length)).word
      case 2 => 
        noun_list(r.nextInt(noun_list.length)).word + verb_list(r.nextInt(verb_list.length)).word
      case 3 => 
        adjective_list(r.nextInt(adjective_list.length))
    }
    println(sentence)
    sentence.toString
  }
}
