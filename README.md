infonom
=======

The Anti-social network. Infonom is a social network for people who dislike
interacting with other people, and would much rather create and comment on the
topics which interest them. In the old parlance, this was called a "blog".
Infonom has the unique qualifier of also being an RSS reader, which enables the
blog/reader to "feel" much more like a social network.

Currently, the app is in an unusable state.

Concepts
========

Reading
-------
 * What should I read first? (based on others' votes, and your votes)
   - Different "sources" which "respond" to the toplevel constitute a vote.
     Votes decay over time. This counts as "voting" by others for Popular
     posts.
   - Does word matching to figure out which posts are most alike, then
     "transfers" votes over to them.
   - You like or share stuff to sort Interesting posts. Votes decay over time.
 * What's the same stuff? (build a mesh network).
   - This creates a heirarchy based on date and reference. Read the source
     article and any children articles which are interesting. Note that an
     "article" (top of the heirarchy) might be read but the "responses" might
     not be.
   - Can also like or share a "source" and this makes their "responses" worth
     more, and will show up.
 * Categories / Search
 * Read / Unread articles
 * Infinite scrolling

 * Share / respond +  Like (Star)
   - The difference between a Share and a Like is that a Like is private and
     a Share is public.

Writing
-------
 * How to write stuff so it's all in one place? (Tweets, Delicious, Articles,
   etc.)
 * Versioning. Individual versions may be public or private.
 * Responses to your articles
 * Sharing / responding to other articles (see - Reading); Adding arbitrary
   URLs.
   - Private = like "delicious / Springpad": personal bookmark sorting. Public
     = sharing or responding

Others' Reading
---------------
 * What did I write recently
 * How to respond to it?
 * What was I referring to?


Design
------

Don't store any GUIDs. 

Articles can be searched by their GUID, as can article data and other pieces of
metadata. We want this to be repeatable for each article, so we can check it.
The metadata is the authoritative way to search for the article's contents. The
article doesn't change, the various soups don't change. The metadata doesn't
change. There's a tags data structure, which changes.

Tags stores things like whether something is bookmarked, starred, or any future
tagging (like commenting).

There's two related structures: The word-soup and the link-soup. The link-soup
is a collection of "substantial" links in the data structure. This means links
that are not "top-level" (i.e. contain things like /something/something.html).
Also, any links from the same domain as yours are also removed. This stops the
kind of spamming of related links and other guff from upvoting itself.

The word-soup is a collection of words along with their word count.

These two soups are used to calculate the "link-score" or "word-score" of an
article using couchdb views. The link-score is calculated by comparing all the
links in all the articles and counting up how many of those correspond to the
URL of the given article. The word-score counts up the number of words in
a given article that are also in starred articles.

linkscore is calculated by retrieving a global link-soup with all the (scored)
links for all articles a week old or younger, then iterating through all the
articles with a link-score-update date older than one day ago, and sticking
a link-score in there if a link exists to that article.

The global link-soup should also list a few "popular" articles, which are not
found among the rss feeds.

word-score is calculated by retrieving a global word-soup with all the
(starred) words for all the articles ever. Because these get heavily cached by
couch, they can be recalculated cheaply. We score articles by adding up the
scores of the words which match the word soup of that article.

