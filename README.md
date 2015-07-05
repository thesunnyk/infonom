# infonom #

The Anti-social network. Infonom is a social network for people who dislike
interacting with other people, and would much rather create and comment on the
topics which interest them. In the old parlance, this was called a "blog".
Infonom has the unique qualifier of also being an RSS reader, which enables the
blog/reader to "feel" much more like a social network.

Currently, the app is in an unusable state.

# Concepts #

## Reading ##
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

## Writing ##
 * How to write stuff so it's all in one place? (Tweets, Delicious, Articles,
   etc.)
 * Versioning. Individual versions may be public or private.
 * Responses to your articles
 * Sharing / responding to other articles (see - Reading); Adding arbitrary
   URLs.
   - Private = like "delicious / Springpad": personal bookmark sorting. Public
     = sharing or responding

## Others' Reading ##
 * What did I write recently
 * How to respond to it?
 * What was I referring to?


## Design ##

Completely re-writing it in scala and in a microservices architecture. This
will ensure that the deployment is *so cumbersome* that it will never have to
work successfully.

### Data types ###

* XML - This is an archival format for storage and backup.
* Filesystem - These are raw HTML files
* H2 database - Microservices tend to use H2 for state.

### Services ###

* *Lettuce* - Handles creating new articles, managing drafts, etc.
* *Tomato* - Publishes new articles to static HTML files.
* *Onion* - Publishes indexing files, including: Categories, Archives, Home
  Page, By Author
* *Carrot* - Holds modelling information for the other services.
* *Ketchup* - SASS/LESS to CSS
* *Jalapeno* - (Optional) ScalaJS
* *Bread* - XML to tomato + onion and vice-versa.

