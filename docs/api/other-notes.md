## Other Notes

### BFD transaction time details

When requesting data from BFD, you must ensure that the `_since` time in the request is after the current BFD transaction time.

The BFD transaction time comes from the Meta object in the bundled response (Bundle.Meta.LastUpdated).

According to FHIR, BFD must return a bundle (which may be empty but still contain the required metadata) even if the patient ID doesn't match.

Therefore, using a fake patient ID which is guaranteed not to match is an easy way to get back a lean response with the valid BFD transaction time:

```json
{
    "resourceType": "Bundle",
    "id": "dc6f27bd-0448-43aa-a067-c120f85199a6",
    "meta": {
        "lastUpdated": "2021-06-08T01:55:08.681+00:00"
    },
    "type": "searchset",
    "total": 0,
    "link": [
        {
            "relation": "self",
            "url": "https://exampleURL/Patient/?_id=blah&_lastUpdated=le2021-06-22T09%3A31%3A28.837731-05%3A00"
        }
    ]
}
```
