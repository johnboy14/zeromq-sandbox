# zeromq-example

Small Application that streams data from EVE Onlines Market Relay [See here](http://eve-market-data-relay.readthedocs.io/en/latest/access.html)

## Usage

    lein repl //start repl
    
    user=> (use 'zeromq-example.core `clojure.core.async)
    
    user=> (def market-chan (market-data)) (def writer-chan (chan 1024))
    
    user=> (start market-chan writer-chan) //writes market data to /tmp/data.txt
    #<ManyToManyChannel clojure.core.async.impl.channels.ManyToManyChannel@7f6e5211>

    user=> (stop market-chan writer-chan) //close both channels

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
