package simpleivr

import java.io.File

import scala.language.higherKinds

import cats.Functor
import cats.free.Free


sealed trait IvrCommand[A] extends Product {
  def fold[F[_]](folder: IvrCommand.Folder[F]): F[A]

  object functor extends IvrCommandF[A] {
    override type Intermediate = A
    override def ivrCommand = IvrCommand.this
    override def apply = identity
  }

  def ivrStep: IvrStep[A] = Free.liftF(functor)
}

object IvrCommand {
  class Folder[F[_]] {
    def default[T]: IvrCommand[T] => F[T] = (cmd: IvrCommand[T]) => throw new MatchError(cmd)
    def streamFile(pathAndName: String, interruptChars: String): F[Char] = default(StreamFile(pathAndName, interruptChars))
    def recordFile(pathAndName: String, format: String, interruptChars: String, timeLimitMillis: Int, offset: Int, beep: Boolean, maxSilenceSecs: Int): F[Char] = default(RecordFile(pathAndName, format, interruptChars, timeLimitMillis, offset, beep, maxSilenceSecs))
    def waitForDigit(timeout: Int): F[Option[Char]] = default(WaitForDigit(timeout))
    def dial(to: String, ringTimeout: Int, flags: String): F[Int] = default(Dial(to, ringTimeout, flags))
    def amd: F[Int] = default(Amd)
    def getVar(name: String): F[Option[String]] = default(GetVar(name))
    def callerId: F[String] = default(CallerId)
    def waitForSilence(ms: Int, repeat: Int = 1, timeoutSec: Option[Int] = None): F[Unit] = default(WaitForSilence(ms, repeat, timeoutSec))
    def monitor(file: File): F[Unit] = default(Monitor(file))
    def hangup: F[Unit] = default(Hangup)
    def setAutoHangup(seconds: Int): F[Unit] = default(SetAutoHangup(seconds))
    def say(sayable: Sayable, interruptDigits: String = ""): F[Option[Char]] = default(Say(sayable, interruptDigits))
    def originate(dest: String, script: String, args: Seq[String]): F[Unit] = default(Originate(dest, script, args))
  }

  case class StreamFile(pathAndName: String, interruptChars: String) extends IvrCommand[Char] {
    override def fold[F[_]](folder: Folder[F]) = folder.streamFile(pathAndName, interruptChars)
  }
  case class RecordFile(pathAndName: String,
                        format: String,
                        interruptChars: String,
                        timeLimitMillis: Int,
                        offset: Int,
                        beep: Boolean,
                        maxSilenceSecs: Int) extends IvrCommand[Char] {
    override def fold[F[_]](folder: Folder[F]) =
      folder.recordFile(pathAndName, format, interruptChars, timeLimitMillis, offset, beep, maxSilenceSecs)
  }
  case class WaitForDigit(timeout: Int) extends IvrCommand[Option[Char]] {
    override def fold[F[_]](folder: Folder[F]) = folder.waitForDigit(timeout)
  }
  case class Dial(to: String, ringTimeout: Int, flags: String) extends IvrCommand[Int] {
    override def fold[F[_]](folder: Folder[F]) = folder.dial(to, ringTimeout, flags)
  }
  case object Amd extends IvrCommand[Int] {
    override def fold[F[_]](folder: Folder[F]) = folder.amd
  }
  case class GetVar(name: String) extends IvrCommand[Option[String]] {
    override def fold[F[_]](folder: Folder[F]) = folder.getVar(name)
  }
  case object CallerId extends IvrCommand[String] {
    override def fold[F[_]](folder: Folder[F]) = folder.callerId
  }
  case class WaitForSilence(ms: Int, repeat: Int = 1, timeoutSec: Option[Int] = None) extends IvrCommand[Unit] {
    override def fold[F[_]](folder: Folder[F]) = folder.waitForSilence(ms, repeat, timeoutSec)
  }
  case class Monitor(file: File) extends IvrCommand[Unit] {
    override def fold[F[_]](folder: Folder[F]) = folder.monitor(file)
  }
  case object Hangup extends IvrCommand[Unit] {
    override def fold[F[_]](folder: Folder[F]) = folder.hangup
  }
  case class SetAutoHangup(seconds: Int) extends IvrCommand[Unit] {
    override def fold[F[_]](folder: Folder[F]) = folder.setAutoHangup(seconds)
  }
  case class Say(sayable: Sayable, interruptDigits: String = "") extends IvrCommand[Option[Char]] {
    override def fold[F[_]](folder: Folder[F]) = folder.say(sayable, interruptDigits)
  }
  case class Originate(dest: String, script: String, args: Seq[String]) extends IvrCommand[Unit] {
    override def fold[F[_]](folder: Folder[F]) = folder.originate(dest, script, args)
  }
}

trait IvrCommandF[A] {
  type Intermediate
  def ivrCommand: IvrCommand[Intermediate]
  def apply: Intermediate => A
  def map[B](g: A => B) = new IvrCommandF[B] {
    type Intermediate = IvrCommandF.this.Intermediate
    def ivrCommand = IvrCommandF.this.ivrCommand
    def apply = (i: Intermediate) => g(IvrCommandF.this.apply(i))
  }

  def fold[F[_]](folder: IvrCommand.Folder[F])(implicit functor: Functor[F]): F[A] =
    functor.map[Intermediate, A](ivrCommand.fold[F](folder))(apply)
}
object IvrCommandF {
  implicit object functor extends Functor[IvrCommandF] {
    override def map[A, B](fa: IvrCommandF[A])(f: A => B) = fa map f
  }
}
