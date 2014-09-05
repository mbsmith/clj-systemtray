(ns clj-systemtray.core-test
  (:import [java.awt SystemTray MenuItem])
  (:require [clojure.test :refer :all]
            [clj-systemtray.core :refer :all]
            [clojure.java.io :refer [resource]]))

(deftest test-menu-item
  (let [item (menu-item :test-item #(true))]
    (is (map? item) "Confirm that the menu-item returns a map.")
    (is (= (count (keys item)) 1) "There should only be one key in the map.")
    (is (= (count (vals item)) 1) "There should only be one value in the map.")
    (is (keyword (first (keys item))) "The map key should contain a keyword.")
    (is (menu-item? item) "menu-item? should recognize the item... as a menu-item.")))

(deftest test-separator
  (let [sep (separator)]
    (is (keyword? sep) "The output should be a keyword.")
    (is (= sep :separator) "The keyword should be :separator.")))

(deftest test-popup-menu
  (is (instance? clojure.lang.PersistentList (popup-menu))
      "Empty popup-menu should be a persistent list.")
  (is (= (count (popup-menu)) 1) "An empty popup-menu should contain only a keyword.")
  (is (keyword? (first (popup-menu))) "The popup-menu should be represented by a keyword.")
  (is (= (first (popup-menu)) :popup) "The popup-menu should be denoted by :popup")
  (is (popup-menu? (popup-menu)) "popup-menu? should recognize an empty popup-menu.")
  (is (= (count (popup-menu "test")) 2)
      "A popup menu should contain :popup, and any other passed arguments.")
  (let [test-popup (popup-menu "one" "two" "three")]
    (testing "Testing popup-menu consistency"
      (is (instance? clojure.lang.Cons test-popup) "Should be of type clojure.lang.Cons")
      (is (= (second test-popup) "one") "Second item should be 'one'")
      (is (= (nth test-popup 2) "two") "Third item should be 'two'")
      (is (= (nth test-popup 3) "three") "Fourth item should be 'three'")
      (is (popup-menu? test-popup) "popup-menu? should recognize a popup-menu."))))


















