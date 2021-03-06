package vggames.git

import scala.util.parsing.combinator.RegexParsers

trait Command {
  def apply(repo : Git, shouldBeTask : Boolean) : Git
  def challenge : String
}

object Command extends RegexParsers {

  private def command : Parser[Command] = "git" ~> (init | add | commit | branch | checkout | merge | rebase | push | pull)

  private def init = "init" ~> id ^^ {
    case repo => Init(repo)
  }

  private def add = "add" ~> "\\S+".r ^^ {
    case path => Add(path)
  }

  private def commit = "commit" ~> (aFlagMessage | messageAFlag | message)

  private def message = "-m" ~ "\"" ~> "[^\"]*".r <~ "\"" ^^ {
    case message => Commit(message)
  }

  private def aFlagMessage = "-a" ~ "-m" ~ "\"" ~> "[^\"]*".r <~ "\"" ^^ {
    case message => Commit(message, true)
  }

  private def messageAFlag = "-m" ~ "\"" ~> "[^\"]*".r <~ "\"" ~ "-a" ^^ {
    case message => Commit(message, true)
  }

  private def branch = "branch" ~> (branchCreateDelete | branchMove)

  private def branchCreateDelete = opt("-D") ~ id ^^ {
    case None ~ branch => Branch(branch)
    case Some(_) ~ branch => DeleteBranch(branch)
  }

  private def branchMove = "-m" ~> id ~ id ^^ {
    case from ~ to => MoveBranch(from, to)
  }

  private def checkout = "checkout" ~> opt("-b") ~ id ^^ {
    case None ~ branch => Checkout(branch)
    case Some(_) ~ branch => Checkout(branch, true)
  }

  private def merge = "merge" ~> id ^^ {
    case branch => Merge(branch)
  }

  private def rebase = "rebase" ~> id ^^ {
    case branch => Rebase(branch)
  }

  private def push = "push" ~> id ~ id ^^ {
    case remote ~ branch => Push(remote, branch)
  }

  private def pull = "pull" ~> id ~ id ^^ {
    case remote ~ branch => Pull(remote, branch)
  }

  private def id = "\\w+".r

  def apply(challenge : String) : Option[Command] = {
    parseAll(command, challenge) match {
      case Success(command, _) => Some(command)
      case a => None
    }
  }
}

case class Commit(name : String, aFlag : Boolean = false) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) : Git = {
    val r = if (repo.branch == "") { repo ~ Checkout("master", true) } else repo
    val newCommits = r.commits.get(r.branch).getOrElse(List[Commit]()) :+ this
    val newFiles = if (aFlag) r.files + ("candidate" -> List()) + ("modified" -> List()) else r.files + ("candidate" -> List())
    r.copy(repo, this, shouldBeTask)(commits = r.commits + (r.branch -> newCommits), files = newFiles)
  }
  def challenge = if (aFlag)
    s"Fa&ccedil;a um commit com a mensagem <code>${name}</code> que tamb&eacute;m inclua os arquivos <strong>modified</strong>"
  else s"Fa&ccedil;a um commit com a mensagem <code>${name}</code>"

  override def toString = name
}

case class Checkout(branch : String, bFlag : Boolean = false) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) : Git = {
    if (repo.commits.keySet.contains(branch))
      return repo.copy(repo, this, shouldBeTask)(branch = branch)
    if (bFlag) {
      val r = repo ~ Branch(branch) ~ Checkout(branch)
      r.copy(repo, this, shouldBeTask)()
    } else repo
  }
  def challenge = if (bFlag) s"Crie o branch <code>${branch}</code> e mude para ele"
  else s"Mude para o branch <code>${branch}</code>"
}

case class Branch(name : String) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) = {
    val commits = repo.commits.get(repo.branch).getOrElse(List[Commit]())
    repo.copy(repo, this, shouldBeTask)(commits = repo.commits + (name -> commits))
  }
  def challenge = s"Crie o branch <code>${name}</code>"
}

case class DeleteBranch(name : String) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) = {
    repo.copy(repo, this, shouldBeTask)(commits = repo.commits - name)
  }
  def challenge = s"Apague o branch <code>${name}</code>"
}

case class MoveBranch(from : String, to : String) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) = {
    val commitList = repo.commits(from)
    repo.copy(repo, this, shouldBeTask)(commits = repo.commits - from + (to -> commitList))
  }
  def challenge = s"Renomeie o branch <code>${from}</code> para <code>${to}</code>"
}

case class Init(repo : String) extends Command {

  def apply(repo : Git, shouldBeTask : Boolean) = repo.copy(repo, this, shouldBeTask)(repo = this.repo)

  def challenge = s"Crie o reposit&oacute;rio <code>${repo}</code>"
}

case class Pull(remote : String, branch : String) extends Command {

  def apply(repo : Git, shouldBeTask : Boolean) = (repo ~ Merge(remote + "/" + branch)).copy(repo, this, shouldBeTask)()

  def challenge = s"Faça pull dos commits de <code>${remote}/${branch}</code> para o seu branch atual."
}

case class Push(remote : String, branch : String) extends Command {

  def apply(repo : Git, shouldBeTask : Boolean) = {
    val remoteBranch = remote + "/" + branch
    val r = repo ~ Checkout(remoteBranch) ~ Merge(repo.branch) ~ Checkout(repo.branch)
    if (r.commits(remoteBranch).last == Commit(s"Merge branch ${repo.branch}")) repo
    else r.copy(repo, this, shouldBeTask)()
  }

  def challenge = s"Envie os commits do seu branch atual para <code>${remote}/${branch}</code>."
}

case class Merge(branch : String) extends Command {

  def apply(repo : Git, shouldBeTask : Boolean) = {
    val branchCommits = repo.commits(repo.branch)
    val otherCommits = repo.commits(branch)
    val base = branchCommits.zip(otherCommits).filter { case (a, b) => a.name == b.name }.map(_._1)
    val commits = branchCommits ++ otherCommits.slice(base.size, otherCommits.size)
    val newCommits = if (branchCommits.size == base.size) commits else commits :+ Commit(s"Merge branch ${branch}")

    repo.copy(repo, this, shouldBeTask)(commits = repo.commits + (repo.branch -> newCommits))
  }

  def challenge = s"Faça o merge dos commits do branch <code>${branch}</code> no branch atual."
}

case class Rebase(branch : String) extends Command {

  def apply(repo : Git, shouldBeTask : Boolean) = {
    val branchCommits = repo.commits(repo.branch)
    val otherCommits = repo.commits(branch)
    val base = branchCommits.zip(otherCommits).filter { case (a, b) => a.name == b.name }.map(_._1)

    val commits = otherCommits ++ branchCommits.slice(base.size, branchCommits.size)

    repo.copy(repo, this, shouldBeTask)(commits = repo.commits + (repo.branch -> commits))
  }

  def challenge = s"Faça o rebase dos commits do branch <code>${branch}</code> no branch atual."
}

abstract class AddFile(path : String, kind : String) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) = {
    val files : List[GitFile] = (repo.files(kind) :+ GitFile(path)).sortWith(_.path < _.path)
    repo.copy(repo, this, shouldBeTask)(files = repo.files + (kind -> files))
  }
  def challenge = throw new RuntimeException("este comando não gera desafios")
}

case class UntrackedFile(path : String) extends AddFile(path, "untracked")
case class ModifiedFile(path : String) extends AddFile(path, "modified")
case class CandidateFile(path : String) extends AddFile(path, "candidate")

case class Add(path : String, folder : Boolean = false) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) = {
    val startPath = if (path == ".") "" else path
    val untracked = repo.files("untracked").filterNot(_.path.startsWith(startPath))
    val modified = repo.files("modified").filterNot(_.path.startsWith(startPath))
    val candidate = repo.files("candidate") ++ repo.files("untracked").filter(_.path.startsWith(startPath)) ++ repo.files("modified").filter(_.path.startsWith(startPath))
    repo.copy(repo, this, shouldBeTask)(files = Map("untracked" -> untracked, "modified" -> modified, "candidate" -> candidate.sortWith(_.path < _.path)))
  }

  def challenge : String = {
    if (path == ".") return "Adicione todos os arquivos da pasta atual &agrave; lista de commit candidate"
    if (folder) return s"Adicione todos os arquivos da pasta <code>${path}</code> &agrave; lista de commit candidate"
    s"Adicione o arquivo <code>${path}</code> &agrave; lista de commit candidate"
  }
}

case class CommitAt(message : String, branch : String) extends Command {
  def apply(repo : Git, shouldBeTask : Boolean) = {
    val commits = repo.commits(branch) :+ Commit(message)
    repo.copy(repo, this, shouldBeTask)(commits = repo.commits + (branch -> commits))
  }
  def challenge = throw new RuntimeException("este comando não gera desafios")
}