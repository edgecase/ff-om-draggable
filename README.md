# ff-om-draggable

A reusable draggable component for [Om](https://github.com/swannodette/om).

# Usage

Add this to your project:

[![Clojars Project](http://clojars.org/ff-om-draggable/latest-version.svg)](http://clojars.org/ff-om-draggable)

```clj
(ns example.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ff-draggable.core :refer [draggable-item]]))

(def app-state (atom {:body "Hello world" :position {:left 100 :top 200}}))

(defn sample-view
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil (app :body)))))

(def draggable-sample-view
  (draggable-item sample-view [:position]))

(om/root
  draggable-sample-view
  app-state
  {:target (. js/document (getElementById "app"))})
```

The key thing to notice here is this function:

```clj
  (draggable-item [view keys])
```

The draggable-item accepts an Om view and sequence containing the path to
the position information to the data given for the view.

In our example `[:position]` resolves to `{:left 100 :top 200}` because the
data given to the `sample-view` is `app`, and `app` contains the position
information at the `:position` key.

# Example

[Sample draggable](http://ff-om-draggable.s3.amazonaws.com/index.html)

# Contributing

ff-draggable-item is still in its early stages so use with caution.

Pull request are welcome.

## License

Copyright © 2014 Neo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
