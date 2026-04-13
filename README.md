# ZIP Streaming Download API

## 1. API Endpoint

- Method: GET
- URL: `/download-zip`

## 2. Query Parameters

| Name   | Type | Required | Description       |
| ------ | ---- | -------- | ----------------- |
| dirSeq | Long | Yes      | Root directory ID |

## 3. Example Request

`GET /download-zip?dirSeq=1`

## 4. Response

- Content-Type: `application/zip`
- Content-Disposition: `attachment; filename="download.zip"`
- File download (streamed)