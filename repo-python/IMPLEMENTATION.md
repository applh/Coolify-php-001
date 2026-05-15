# Python CMS Implementation

A server-side implementation using FastAPI and Jinja2 templates, mirroring the "classic" server-side feel of PHP but with modern Python benefits.

## Key Files
- `main.py`: The FastAPI application. Uses request headers to identify the host.
- `templates/site.html`: A base Jinja2 template that renders dynamic blocks.
- `sites.json`: Site database (JSON).

## Multi-Domain Logic
```python
host = request.headers.get("host", "").split(":")[0]
active_site = next((s for s in sites if s["domain"] == host), sites[0])
```

## DX Benefits
- **Type Hints**: Excellent IDE support.
- **FastAPI**: Inherent speed and automatic documentation.
