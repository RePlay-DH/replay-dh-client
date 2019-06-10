# RePlay-DH Client

Integrated Tool for tracking, versioning and publishing workflows.

- Document your workflows on the fly while working
- Have all your workflow data backed up in a local Git repository
- Export metadata or data from your workflow in various formats
- Publish directly to institutional repositories such as [DSpace](https://www.duraspace.org/dspace/ "DSpace's Homepage") from within the client

See the original [project homepage](https://www.ub.uni-stuttgart.de/replay) for additional information and associated publications.

### Setup

The RePlay-DH Client does not require installation of any local software other than Java version 8 or newer. To begin using the client, download one of the [releases](https://github.com/RePlay-DH/replay-dh-client/releases) available in the code section on our GitHub page (or alternatively you can also build and package the client locally with maven).

Unpack the downloaded zip to the location of your choice (let's call this "client folder" from here on) and run the contained replay-dh-client-<version>.jar file.

Note that the client folder can be shared between users, since settings, logs and cached data are all stored in a folder within the user's home directory by default. This is especially useful when adding external plugins, so that they are usable by multiple people immediately.

### Usage

After starting the client for the first time you will be guided through several setup wizards that eventually let you choose the initial workspace folder to be used with the client. The majority of UI components should be fairly self-explanatory, but we will add either an in-depth documentation to the wiki or provide it as a manual.pdf to be bundled with every client release.

### Advanced Configuration

Besides the configuration options available from within the client you can use a set of command line arguments when starting to override certain settings or change the client behavior. For a usually more up to date list of supported parameters check the Javadoc for the `bwfdm.replaydh.core.RDHClient` class.

The following table lists currently supported parameters:

|           Option            |    Type     |              Variants                | Description |
|:--------------------------- |:----------- |:------------------------------------ |:----------- |
| -v                          | flag        | -Dintern.verbose=true                | Causes the client to write additional logging information. |
| -dev                        | flag        | -Dintern.debug=true                  | Starts the client in developer mode, making available additional debugging functions. |
| -config &lt;name&gt;        | string      | - | Name of the file containing additional settings. This file will also be used to save changes made by the user. |
| -dir &lt;file&gt;           | path string | -Duser.folder=&lt;file&gt;           | Path to the client folder where settings and other client internal informations get stored. |
| -workspace &lt;file&gt;     | path string | -Dclient.workspace.path=&lt;file&gt; | Path to the currently active working directory. |
| -D&lt;key&gt;=&lt;value&gt; | strings     | -                                    | Allows to override client settings (this is a basic java command line feature and listed here simply for the sake of completeness, as the client reads in the full set of currently set Java properties as base of the internal settings, with higher priority that the read in config file). |

### Known Issues

Nothing major for now...

### Contribution

If you want to convert the md file to html4 (needed for the client docu feature), please use pandoc command:

	pandoc -f markdown -t html4 client-docu.md -o client-docu.html

### License

The client code is licensed under MIT

See the `license.txt` file for additional info regarding licensing.
