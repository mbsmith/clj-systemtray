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

	  [clj-systemtray "0.2.1"]

Usage is pretty straight forward.  Menus are described using composable functions.

    (popup-menu
        (menu-item :title fcn)
        (menu-item "another title" another-fn)
        (separator)
        (menu "more options"
            (menu-item "deep item 1" fcn)
            (menu-item "deep item 2" fcn)
            (separator)
            (menu-item :deep-item-3 fcn))
        (menu-item :exit-title exit-fn))

This can then be used with the tray icon like so:

     (make-tray-icon! path-to-icon popup-menu)

![image](https://cloud.githubusercontent.com/assets/56411/6353164/9df71070-bc15-11e4-9114-e22e1e1e450d.png)

## License

Copyright © 2014 Micheal Smith

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
