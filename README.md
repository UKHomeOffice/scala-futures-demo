Scala Futures Demo
==================
Demo of Scala Futures at a high level - Get the basics of using Futures correctly.

Application built with the following (main) technologies:

- Scala

- SBT

- Specs

Introduction
------------
TODO

Build
-----
The project is built with SBT. On a Mac (sorry everyone else) do:
> brew install sbt

It is also a good idea to install Typesafe Activator (which sits on top of SBT) for when you need to create new projects - it also has some SBT extras, so running an application with Activator instead of SBT can be useful. On Mac do:
> brew install typesafe-activator

To compile:
> sbt compile

or
> activator compile

To run the specs:
> sbt test

How Not to use Futures
----------------------
The original Registered Traveller Customer application is a prime example of how not to use Scala Futures.
At the time of writing, there are some issues in the Registered Traveller Caseworker application, and we shall highlight some examples here that have been fixed.

In "corecaseworker" WICrossCheckDownloadServiceSpec, there was the following code:

```scala
wiCrossCheckDownloadService.generateDownload("tester@tester.com")
there was one(crossCheckService).setCheckToInProgress(casesReadyForTravelHistory)

```

Where generateDownload results in a Future[ProcessingStatus]

This was subsequently changed to:

```scala
wiCrossCheckDownloadService.generateDownload("tester@tester.com") must beLike[ProcessingStatus] {
  case ProcessingStatus(ProcessingStatus.SUCCESS, 5, 3, Nil) =>
    there was one(crossCheckService).setCheckToInProgress(casesReadyForTravelHistory)
}.awaitFor(10 seconds)
```