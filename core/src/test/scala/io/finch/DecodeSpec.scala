package io.finch

import com.twitter.finagle.http.Request
import com.twitter.util.{Await, Return, Try}
import org.scalatest.{FlatSpec, Matchers}

import scala.math._
import scala.reflect.ClassTag

class DecodeSpec extends FlatSpec with Matchers {

  private def decode[A](json: String)(implicit d: DecodeRequest[A]): Try[A] = d(json)
  
  "A DecodeJson" should "be accepted as implicit instance of superclass" in {
    implicit object BigDecimalJson extends DecodeRequest[BigDecimal] {
      def apply(s: String): Try[BigDecimal] = Try(BigDecimal(s))
    }

    decode[BigDecimal]("12345.25") shouldBe Return(BigDecimal(12345.25))
  }
  
  "A RequestReader for a String" should "allow for type conversions based on implicit DecodeRequest" in {
    val request: Request = Request(("foo", "5"))
    val reader: RequestReader[Int] = param("foo").as[Int]
    val result = reader(request)
    Await.result(result) shouldBe 5
  }
  
  it should "fail if a type conversions based on implicit DecodeRequest fails" in {
    val request: Request = Request(("foo", "foo"))
    val reader: RequestReader[Int] = param("foo").as[Int]
    val result = reader(request)
    Await.result(result.liftToTry).isThrow shouldBe true
  }
  
  it should "allow for type conversions of optional parameters" in {
    val request: Request = Request(("foo", "5"))
    val reader: RequestReader[Option[Int]] = paramOption("foo").as[Int]
    val result = reader(request)
    Await.result(result) shouldBe Some(5)
  }
  
  it should "fail if a type conversions for an optional value fails" in {
    val request: Request = Request(("foo", "foo"))
    val reader: RequestReader[Option[Int]] = paramOption("foo").as[Int]
    val result = reader(request)
    Await.result(result.liftToTry).isThrow shouldBe true
  }
  
  it should "skip type conversion and succeed if the optional value is missing" in {
    val request: Request = Request(("bar", "foo"))
    val reader: RequestReader[Option[Int]] = paramOption("foo").as[Int]
    val result = reader(request)
    Await.result(result) shouldBe None
  }
  
  it should "allow for type conversions of a parameter list" in {
    val request: Request = Request(("foo", "5,6,7"))
    val reader: RequestReader[Seq[Int]] = params("foo").as[Int]
    val result = reader(request)
    Await.result(result) shouldBe Seq(5,6,7)
  }
  
  it should "fail if a type conversion for an element in a parameter list fails" in {
    val request: Request = Request(("foo", "5,foo,7"))
    val reader: RequestReader[Seq[Int]] = params("foo").as[Int]
    val result = reader(request)
    Await.result(result.liftToTry).isThrow shouldBe true
  }
  
  it should "skip type conversion and succeed if a parameter list is empty" in {
    val request: Request = Request(("bar", "foo"))
    val reader: RequestReader[Seq[Int]] = params("foo").as[Int]
    val result = reader(request)
    Await.result(result).isEmpty shouldBe true
  }
}
