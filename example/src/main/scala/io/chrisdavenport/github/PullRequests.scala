package io.chrisdavenport.github
import cats.implicits._
import cats.effect._
import org.http4s.client.blaze.BlazeClientBuilder
import io.chrisdavenport.github.endpoints.PullRequests


object PullRequestsExample extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    for {
      c <- BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.global).resource
      out <-
          PullRequests.pullRequestsFor[IO](
            "ChristopherDavenport",
            "github",
            None
          )
          .run(c)
          .take(2)
          .compile
          .resource
          .toList
    } yield out
    
  }.use(requests => 
    IO(println(requests)).as(ExitCode.Success)
  )

}