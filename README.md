# GWT M2E Connector

Manages Supersource and Resource Folders for GWT-Projects.
This is a GitHub-export of https://code.google.com/p/gwt-m2e/ which was once maintained by Stafan Wokusch.

The current version 1.0.0.4 supports m2e 1.7 and thus eclipse Mars.

## Features

**Supersource:** It adds the Supersource-Folder to the Buildpath to remove Eclipse-Compiler-Errors for wrong Package-declarations.

**Resource Bug:** Using @Source in Maven Projects, causes a Bug, because the Eclipse-Plugin dont see resources in the resource Folders of Maven. To fix this, this plugin removes the exclusion of all resource-folders for Eclipse. http://code.google.com/p/google-web-toolkit/issues/detail?id=4600

## Limitations

For now, the Plugin only supports <super-source/> without path Arguments. (Patches welcome)

**Workarround:** add new emul.gwt-xml with <super-source/> directly inside your "emul"-Folder and include it in your <main>.gwt.xml.

## Usage

The Plugin starts in the "Maven->Update Project Configuration..." cycle. (Rightclick on the Project->Maven->Update Project Configuration...).

The Plugin goes active for any Maven Project that uses maven-gwt-plugin:resource.

IMPORTANT: The Plugin onlys works, when you don't ignore the M2E-Connector for maven-gwt-plugin. When you have added this ignore to your pom.xml's, you need to remove that.

## Installation

### Eclipse-Marketplace:

http://marketplace.eclipse.org/content/gwt-m2e-connector

### Update site:

https://github.com/levigo/gwt-m2e/raw/master/updateSite/

## Changes

* 1.0.0.4: improved super-source handling