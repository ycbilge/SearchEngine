SearchEngine
============

Lucene Based Search Engine


In this project, Apache Lucene version 4.4.0 is used to build a search engine with schema-independent index that can support boolean queries with
proximity ranking.

Run : 

java Indexer \<dirPath\> \<indexPath\>
java Searcher \<indexPath\> \<OP\> \<terms\>

\<OP\> can be AND, OR.

For example ;
java Searcher INDEXPATH -or apple store iphone
