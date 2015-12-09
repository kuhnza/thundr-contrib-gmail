thundr-contrib-gae-blog [![Build Status](https://travis-ci.org/3wks/thundr-contrib-gae-blog.svg)](https://travis-ci.org/3wks/thundr-contrib-gae-blog)
=======================


A thundr module for presenting Blogger content on GAE apps

This module provides services to access blog content authored on the [Blogger](http://www.blogger.com/) platform and show the content in a Google AppEngine application.


Overview
--------

This module allows you to author and adminster blog content using Google's [Blogger](http://www.blogger.com/) platform and then access the blog posts via the [Blogger API](https://developers.google.com/blogger/docs/3.0/getting_started). Typically you would use this to show the blog posts on your GAE web app without the need to build your own blog authoring tools.


Setup
-----

To install the module include the dependency in your ApplicationModule. For example:

    @Override
    public void requires(DependencyRegistry dependencyRegistry) {
        super.requires(dependencyRegistry);
        dependencyRegistry.addDependency(GaeBlogModule.class);
    }

This will do the following:

- inject a `com.threewks.thundr.gae.blog.service.BlogService` into your injection context
- add the following routes
  - /admin/blog/setup
  - /admin/blog/setup/oauth2callback

You will also need to create a Google client id for your web application in order to access the content from Blogger. This can be done from the 'Credentials' screen on the [Google Developers Console](https://console.developers.google.com).

Note: be sure to configure the redirect URIs and Javascript origins and to configure the consent screen with the required information.


Configuration
-------------

The following **mandatory** configuration options must be set in your application.properties file:

- bloggerBlogId - the ID of your blog (from Blogger)
- bloggerOAuthClientId - the client id (from the Google Developer Console)
- bloggerOAuthClientSecret - the client secret (from the Google Developer Console)

The following **optional** configuration options can be set in your application.properties file:

- blogAdminRootPath - you can optionally override the root path to the blog admin routes. By default the root is /admin/blog


Authorising Access
------------------

Before you can use the module you must first authorise access to your blog via OAuth. To do this run your application and request the following route:

- /admin/blog/setup

This will redirect you to Google where you must login and authorise access to your blog. Once complete you will be directed back
to your application and shown a success message. The OAuth credentials are stored the data store in the 'StoredCredential' entity
which is managed by the Google API.

NOTE: it is possible (and recommended) to configure the blog to be private and only accessible by blog authors. This will ensure
that your blog content is **not** available at http://&lt;yourblog&gt;.blogspot.com. This is beneficial for your Google page rank because
Google will penalise you if you have the exact same content on multiple sites (ie: on blogspot.com as well as on your AppEngine app).

By making the blog private the API will be able to access the blog content, but it will not be possible for anyone else to see it.
  
This can be configured from the Blogger settings under 'Basic' and modifying the 'Blog Readers' setting to be private. 

Usage
-----

BlogService provides a simple wrapper for the underlying Google Blogger API. Some example usages are shown below:

List recent blog posts:

    PostList postList = blogService.listPosts();
    for (Post post : postList.getItems()) {
        System.out.println(post.getTitle());
    }
    
    
Retrieve a specific post:

    Post post = blogService.getPost("2015", "05", "my-blog-post");
    System.out.println(post.getTitle());
