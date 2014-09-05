;; ## A Clojure interface to the Java AWT SystemTray facilities.
;;
;; This is by no means a comprehensive interface.  However it should be suitable for
;; many use cases.  Some things are not currently supported.  Such as complicated
;; popup menus.
;;
;; One of the main benefits of using this is it allows describing of tray menus
;; in a composable fashion.  Which should make working with the SystemTray much
;; more seamless for clojure developers.
;;
(ns clj-systemtray.core
  (:import [java.awt SystemTray TrayIcon Toolkit PopupMenu MenuItem]
           [java.awt.event ActionListener]))

;; ## Platform checks

(defn tray-supported?
  "What good is a system tray library on a platform that doesn't support the
   system tray?  This returns true if the tray is supported."
  []
  (. SystemTray isSupported))

(defn- tray-or-throw!
  "When a check isn't enough an exception may be called for. Throw an exception
   if a tray isn't found.  Should generally be used within any function that
    actually performs actions on the system tray."
  []
  (when-not tray-supported?
    (throw (Exception. "SystemTray not supported on this platform."))))

;; ## Defining the menu
;;
;; The three main building blocks of a system tray menu are the popup menu
;; itself, menu items, and separators.  There can often be much more to them
;; than that, but I've decided to keep things simple for now.  Here is an
;; example of a small popup menu using the menu definitions.
;;
;; It should be noted that currently submenus are not supported, but may be
;; in the near future.
(defn popup-menu?
  [v]
  (if (and (coll? v) (not (map? v)))
    (and (keyword?  (first v))
         (= (name (first v)) "popup"))
    (and (keyword? v)
         (= (name v) "popup"))))

(defn popup-menu
  "Every menu definition should be enclosed within a popup-menu expression.
   Currently only one is supported per menu.  Just for clarity's sake here is
   an example menu definition.

     (popup-menu
       (menu-item :title fn)
       (menu-item :another-title another-fn)
       (separator)
       (menu-item :exit-title exit-fn))"
  [& args]
  (cons :popup args))

(def menu-item? map?)

(defn menu-item
  "Most of the menu is going to be composed of menu-items.  Each one has a title
   and a corresponding function.  The function is *not* mandatory however.  The
   title should be a keyword;  E.g. :title."
  [title fn]
  {title fn})

(defn separator
  "Outputs a menu separator."
  []
  :separator)
(defn separator? [v] (and (keyword? v)
                          (= (name v) "separator")))

;; ## Processing the menu

(defn- add-listener!
  "Assigns a listener to a menu, or menuitem.  The function is called on a given event.
   The provided function should take one argument which is the event itself."
  [menu fn]
  (doto menu
    (.addActionListener
     (proxy [ActionListener] []
       (actionPerformed [event]
         (fn event))))))

(def test-menu (popup-menu
                (menu-item "test1" #(true))
                (separator)
                (menu-item "test2" #(true))))

(defn- process-menu
  "We need a function that can recursively process the popup-menu forms created
   by the functions above.  This is such a function.  This iterates over the
   given forms, and outputs a collection of menu related AWT objects.  The first
   object in the collection should *always* be the object for the popup menu
   itself.

   The first, and only argument to this function is a menu constructed by the
   functions above."
  [menu-data]
  (letfn [(add-menu [popup title fn]
            (let [item (add-listener! (MenuItem. title) fn)]
              (.add popup item)
              (list item)))
          (read-menu [menu popup acc]
            (cond (or (nil? menu) (empty? menu)) acc
                  (popup-menu? (first menu))
                  (let [new-popup (PopupMenu.)]
                    (read-menu (rest menu)
                               new-popup
                               (concat acc (list new-popup))))
                  (menu-item? (first menu))
                  (read-menu (rest menu)
                             popup
                             (concat acc
                                     (add-menu popup
                                               (name (first (keys (first menu))))
                                               (first (vals (first menu))))))
                  (separator? (first menu))
                  (do
                    (.addSeparator popup)
                    (read-menu (rest menu) popup acc))))]
    (read-menu menu-data nil nil)))

;; ## Dealing with the tray

(defn make-tray-icon!
  "Now with the previous functions defined we can finally create the tray icon.
   This function takes two arguments.  The first one being the path to the icon
   image that will be displayed.  The second is menu layout composed using the
   functions above.  Both arguments are completely mandatory if you expect the
   tray icon to function as expected.

   For now the layout menu must always be enclosed in a popup menu, and should
   contain *at least* one menu item.  An example menu follows.

       (popup-menu
         (menu-item :title fn))

   This returns a tray icon object.  Which will prove useful should you want
   to update, or remove the tray icon."
  [icon-path menu]
  (tray-or-throw!)
  (let [tray (SystemTray/getSystemTray)
        tray-icon (TrayIcon. (.getImage (Toolkit/getDefaultToolkit)
                                        icon-path))]
    (when menu
      (.setPopupMenu tray-icon (first (process-menu menu))))
    (.setImageAutoSize tray-icon true)
    (.add tray tray-icon)
    tray-icon))

(defn remove-tray-icon!
  "Of course we may need to remove a tray icon from time to time.  Should that
   need arise we have this function.  It's pretty straight forward."
  [tray-icon]
  (.remove (SystemTray/getSystemTray) tray-icon))

;; ## Useful functions

(defn display-message
  "The AWT system tray provides the handy ability to display a message near a
   tray icon.  Unfortunately it doesn't always seem to work depending on the
   platform.  So your mileage may vary when it comes to this function.

   The function takes three arguments.  A caption which functions as a sort of
   message title, the message itself, and the message type.  The message type
   is going to be :none, :info, :warning, or :error."
  [tray-icon caption message type]
  (let [msg-type (case type
                   :none java.awt.TrayIcon$MessageType/NONE
                   :info java.awt.TrayIcon$MessageType/INFO
                   :warning java.awt.TrayIcon$MessageType/WARNING
                   :error java.awt.TrayIcon$MessageType/ERROR)]
    (.displayMessage tray-icon caption message msg-type)))













