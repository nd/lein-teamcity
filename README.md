# lein-teamcity

A Leiningen plugin for on-the-fly stages, tests and artifacts
reporting in TeamCity.

## Usage

create a template at `~/.lein/profiles.d/teamcity.clj` with the
following content:

    {
      :plugins [[lein-teamcity "0.2.1"]]
      :monkeypatch-clojure-test false
    }

and run `lein with-profile +teamcity do clean, test, jar`.

Tests reporting requires leiningen 2.3.1+.