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
  (:import [java.awt SystemTray TrayIcon Toolkit PopupMenu MenuItem Menu]
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
;; The four main building blocks of a system tray menu are the popup menu
;; itself, menus, menu items, and separators.  There can often be much more to them
;; than that, but I've decided to keep things simple for now.  Here is an
;; example of a small popup menu using the menu definitions.
;;
(defn popup-menu?
  [v]
  (if (and (coll? v) (not (map? v)))
    (and (keyword?  (first v))
         (= (first v) :popup))
    (and (keyword? v)
         (= v :popup))))

(defn popup-menu
  "Every menu definition should be enclosed within a popup-menu expression.
   Currently only one is supported per menu.  Just for clarity's sake here is
   an example menu definition.

     (popup-menu
       (menu-item :title fn)
       (menu-item \"another title\" another-fn)
       (separator)
       (menu \"more options\"
         (menu-item \"deep item 1\" fn)
         (menu-item \"deep item 2\" fn))
       (menu-item :exit-title exit-fn))"
  [& args]
  (cons :popup args))

(defn menu?
  [v]
  (if (and (coll? v) (not (map? v)))
    (and (keyword?  (first v))
         (= (first v) :menu))
    (and (keyword? v)
         (= v :menu))))

(defn menu
  "Nested menus are possible. Each one has a title and contents. The title must be a string or keyword."
  [title & contents]
  (concat [:menu title] contents))

(def menu-item? map?)

(defn menu-item
  "Most of the menu is going to be composed of menu-items.  Each one has a title
   and a mandatory corresponding function. The title must be a string or keyword."
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
  (letfn [(add-menu-item [parent title fn]
                         (let [item (add-listener! (MenuItem. (name title)) fn)]
                           (.add parent item)
                           parent))
          (add-menu [parent title]
                    (let [menu (Menu. (name title))]
                      (.add parent menu)
                      menu))

          (read-menu [descriptions parent-menu]
                     (cond
                       ; base case
                       (or (nil? descriptions) (empty? descriptions))
                       parent-menu

                       ; root case
                       (popup-menu? (first descriptions))
                       (let [new-popup (PopupMenu.)]
                         (read-menu (rest descriptions)
                                    new-popup))

                       ; nested menu case
                       (menu? (first descriptions))
                       (do
                         (read-menu (drop 2 (first descriptions))
                                    (add-menu parent-menu
                                              (second (first descriptions))))
                         (read-menu (rest descriptions)
                                    parent-menu))

                       ; menu item case
                       (menu-item? (first descriptions))
                       (read-menu (rest descriptions)
                                  (add-menu-item parent-menu
                                                 (first (keys (first descriptions)))
                                                 (first (vals (first descriptions)))))

                       ; separator case
                       (separator? (first descriptions))
                       (do
                         (.addSeparator parent-menu)
                         (read-menu (rest descriptions) parent-menu))))]
    (read-menu menu-data nil)))

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
                                        ^String icon-path))]
    (when menu
      (.setPopupMenu tray-icon (process-menu menu)))
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
