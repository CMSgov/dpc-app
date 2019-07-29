# Building the Implementation Guide

Per the FHIR specific, each application should have and Implementation Guide (IG) that describes the possible server operations, and data formats/responses.

Building this guide is a two step process.

1. Author the content 

1. Build the IG

## Required dependencies

The FHIR IG publisher is required for the build process, but will automatically be installed during the build process.

The publisher has a dependency on [Jekyll](https://jekyllrb.com), which needs to be installed separately:
`gem install jekyll`.


## Author the content

### Adding content to the IG

When adding a new resource (StructureDefinition, etc) to the IG, we need to specify it in two places. The first is in the [cms-dpc-ig.xml](ig/source/resources/implementationguide/cms-dpc-ig.xml), as an additional `resource`.

The `example` key denotes whether this resource is an example data structure, or a generic reference. Each resource needs a `sourceReference` which defines how the resource is laid out in the IG and the name of the referenced file on disk.

```xml
<package>
    <name value="Profiles in this implementation guide"/>
    ...
    <resource>
        <example value="false"/>
            <sourceReference>
                <reference value="CapabilityStatement/dpc-capabilities-statement"/>
                <display value="capabilities.json"/>
        </sourceReference>
    </resource>
    ...
</package>
```
Once the resource has been added to the `cms-dpc-ig.xml` file, we also need to add it to the [ig.json](ig/ig.json) file, which serves as the *control* file for the publisher that gets run in the next step. This defines the layout of the generated html files, as well as the existing templates that are used to control content and formatting. It's important that the name of the resource, and the `source` element match what's specified in the xml file.

```javascript
"resources": {
    ...
    "CapabilityStatement/dpc-capabilities-statement": {
                "base": "CapabilityStatement-dpc-capabilities.html",
                "defns": "CapabilityStatement-dpc-capabilities-definitions.html",
                "source": "capabilities.json"
            }
    ...
}
```

The final step is to add the individual Markdown files which contain the descriptive content.
In the `pages/includes` directory we need to create three files which specify the intro, summary and search content for each resource:

`touch {dpc-profile-name}-{intro|summary|search}.md`

You also need to update the index pages (e.g. profiles.md, capstatements.md) with the link to the newly created resource.
This is the auto-generated file specified in the `base` field of the `ig.json` file. 

## Build the IG

The FHIR group has created an [IG Publisher](http://wiki.hl7.org/index.php?title=IG_Publisher_Documentation) which automates the process of generating the HTML files and documents in the layout expected by FHIR developers.

The IG publisher is a Java application that can be downloaded from the HL7 (link above), or automatically when running the `make ig/publish` command.

It works by reading the control file (ig.json) from the `ig/` folder, collecting all the defined resources, and generating the HTML files.

The input and output directories are specified in the `ig.json` file.

The publisher generates an `output/`directory, which contains all the statically compiled assets for the IG. It also creates all of the various supporting documentation that needs to be present.

In addition, it generates a `qa.html` file, which contains a validation report as to the correctness of the various definitions and resources.