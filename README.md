# clj-systemtray

[![Build Status](https://travis-ci.org/mbsmith/clj-systemtray.svg)](https://travis-ci.org/mbsmith/clj-systemtray)

A Clojure interface to the Java AWT SystemTray facilities.  This strives to provide
a simple, functional way to interact with the system tray on various platforms.

## Documentation

For full documentation see http://mbsmith.github.io/clj-systemtray

## Current Version (in case I forget)
[![Clojars Project](http://clojars.org/clj-systemtray/latest-version.svg)](http://clojars.org/clj-systemtray)

## Usage

To obtain and use this in your project (via leiningen 2) put the include the
following in your project.clj

	  [clj-systemtray "0.1.4"]

Usage is pretty straight forward.  Menus are described using composable functions.

        (popup-menu
		(menu-item :title fn)
		(menu-item :another-title another-fn)
		(separator)
		(menu-item :Exit exit-fn))

This can then be used with the tray icon like so:

     (make-tray-icon! path-to-icon popup-menu)

## License

Copyright © 2014 Micheal Smith

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
