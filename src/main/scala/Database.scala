package org.pii5656.morphologicalanalysis
import scala.concurrent.{Await, Future}
import scala.slick.driver.SQLiteDriver.api._
import slick.lifted.TableQuery
import slick.jdbc.GetResult
import slick.jdbc.JdbcBackend.Database;
import org.sqlite.JDBC


object DB {
  //val db = Database.forURL("jdbc:sqlite:searchindex.db", driver = "org.sqlite.JDBC")
  val db = Database.forConfig("db")
  case class Word(word: String, pofspeech: String, count: Int)
  class Words(tag: Tag) extends Table[Word](tag, "Words") {
    def word = column[String]("word")
    def pofspeech = column[String]("pofspeech")
    def count = column[Int]("count")
    def * = (word, pofspeech, count) <> (Word.tupled, Word.unapply)
  }
  //val words = TableQuery[Words]
  object words extends TableQuery(new Words(_))

  def createTables(db: Database): Future[Int] = {
    db.run(sqlu"""create table Words(rowid integer primary key, word, pofspeech, count)""")
  }

  def insertWord(word: Word): Future[Int] =
    db.run(words += word)

  def updateWordCount(word: Word, count: Int): Future[Int] = 
    db.run(words.filter(_.word === word.word).map(_.count).update(count))

  def getSameWords(word: Word): Future[Seq[Word]] = {
    db.run(words.filter(_.word === word.word).result)
  }
}
