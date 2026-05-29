import json
import os
import platform
from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.templating import Jinja2Templates

app = FastAPI()

# Setup templates
templates = Jinja2Templates(directory="templates")

def get_sites_from_content(content_path):
    sites = []
    if os.path.exists(content_path):
        for item in os.listdir(content_path):
            if item in ('.', '..', 'my-data'): continue
            if os.path.isdir(os.path.join(content_path, item)):
                sites.append(item)
    return sites

def get_plugins():
    plugins = []
    plugin_path = os.path.join(os.path.dirname(__file__), 'plugins')
    if os.path.exists(plugin_path) and os.path.isdir(plugin_path):
        for item in os.listdir(plugin_path):
            if item in ('.', '..'): continue
            if os.path.isdir(os.path.join(plugin_path, item)):
                plugins.append(item)
    return plugins

def validate_setup(content_path):
    return {
        'content_path': content_path,
        'is_writable': os.access(content_path, os.W_OK) if os.path.exists(content_path) else False,
        'sites': get_sites_from_content(content_path),
        'available_plugins': get_plugins(),
        'python_version': platform.python_version(),
        'server_software': 'uvicorn'
    }

# Load sites data
def load_sites():
    try:
        with open("sites.json", "r", encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return []

@app.middleware("http")
async def cms_debug_middleware(request: Request, call_next):
    if request.query_params.get("cms_debug") == "true":
        content_path = os.path.join(os.path.dirname(__file__), 'content')
        data = validate_setup(content_path)
        return JSONResponse(content=data)
    response = await call_next(request)
    return response

@app.get("/favicon.ico", include_in_schema=False)
async def favicon():
    return HTMLResponse(content="", status_code=204)

@app.get("/", response_class=HTMLResponse)
async def read_site(request: Request):
    try:
        sites = load_sites()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to load sites: {str(e)}")
    
    # Domain detection
    host = request.headers.get("host", "").split(":")[0]
    site_override = request.query_params.get("__site") or os.environ.get("ACTIVE_SITE_OVERRIDE")
    
    active_site = next(
        (s for s in sites if s["domain"] == host or str(s.get("id")) == site_override or s.get("domain") == site_override),
        sites[0] if sites else None
    )
    
    if not active_site:
        raise HTTPException(status_code=404, detail="No sites configured")
        
    return templates.TemplateResponse(
        request=request,
        name="site.html",
        context={"site": active_site}
    )

@app.get("/api/sites")
async def get_sites():
    return load_sites()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3000)
