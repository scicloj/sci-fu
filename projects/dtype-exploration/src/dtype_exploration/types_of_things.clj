(ns dtype-exploration.types-of-things
  (:require [tech.v3.datatype :as dtype]
            [notespace.api]
            [notespace.kinds :as kinds]))

#^kinds/md-nocode
(str "## Types of Things in tech.datatype")

^kinds/md-nocode
(str "Some notes from https://cnuernber.github.io/dtype-next/overview.html:
* types of things
  * buffer - efficient primitive-datatype-aware access
  * reader - a buffer that can be read from
  * writer - a buffer that can be written to
  * container - ?
* Most things can be converted to readers of a specific type. I.e. persistent vectors, java arrays, numpy arrays, etc can be converted to readers.
* Readers implement List and RandomAccess so they look like persistent vectors in the repl.
* Any reader can be \"reshaped\" into a tensor.")


^kinds/md-nocode
(str "#### Looking at the characteristics of some of the types")

;; A reader
(def a-reader 
  (-> [1 2 3 4 5]
      (dtype/->reader)))
a-reader

^kinds/md-nocode
(str "A reader is, of course, a reader, and it is iterable?")

(dtype/reader? a-reader)
(dtype/iterable? a-reader)

^kinds/md-nocode
(str "But it's also a writer? Why?")

(dtype/writer? a-reader)

^kinds/md-nocode
(str "Now looking at writers, you can't create a writer from a persistent vector the way you can with a reader.")

(-> [1 2 3]
    type)

^kinds/md-nocode
(str "This code will throw an error...")

(delay (-> [1 2 3]
           (dtype/->writer)))

^kinds/md-nocode
(str "But you can do:")

(-> [1 2 3]
    (dtype/->reader)
    (dtype/->writer))

^kinds/md-nocode
(str "Or, from a buffer...")

(-> [1 2 3]
    (dtype/->buffer)
    (dtype/->writer))

^kinds/md-nocode
(str "#### Containers")

(def a-native-heap-container
  (dtype/make-container :native-heap :float32 (range 5)))

(def a-jvm-heap-container
  (dtype/make-container :jvm-heap :float32 (range 5)))

(type a-native-heap-container)
(type a-jvm-heap-container)

^kinds/md-nocode
(str "Properties of native heap container...")

(-> a-native-heap-container
    (dtype/reader?))

(-> a-native-heap-container
    (dtype/iterable?))

(-> a-native-heap-container
    (dtype/writer?))

^kinds/md-nocode
(str "Properties of jvm heap container...")

(-> a-jvm-heap-container
    (dtype/reader?))

(-> a-jvm-heap-container
    (dtype/iterable?))

(-> a-jvm-heap-container
    (dtype/writer?))

^kinds/md-nocode
(str "Why are containers readers?")

^kinds/md-nocode
(str "What are all the primitive type functions")


dtype/->array
dtype/->array-buffer
dtype/->reader
dtype/->writer
dtype/->buffer
dtype/make-container
dtype/make-list
dtype/make-reader

(.isArray (class (dtype/->array [1 2 3])))



