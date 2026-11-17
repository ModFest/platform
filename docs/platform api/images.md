# Images
Platform can store images, which are meant to be accessible through a url.
How this url is generated is dependent on the environment.

In local development environments, platform will host the files itself (see the `ImageCdn` controller).
When a request is made, it will determine the hostname used to access platform to figure out where its being
hosted, then it'll construct a url to itself. Note that when platform is sending, for example, discord webhooks
then there's no request, and it can't figure out a url.

In production, the `PLATFORM_IMAGECDNURL` environment variable is set. This stops platform from hosting images
by itself, and instead that url is used. For example on the live server that environment variable is set to
`https://modfest.net/pfimages/`. We have nginx configured to serve the `images` folder inside platform's data
on that URL.

If you'd want to you could set that environment variable locally too. Though the main use for it would be discord
webhooks, which won't work with any localhost url anyway. You'd have to set up a proper server to test those.
