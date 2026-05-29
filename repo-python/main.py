import json
import os
import platform
from fastapi import FastAPI, Request, HTTPException, Depends
from fastapi.responses import HTMLResponse, JSONResponse, StreamingResponse
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles

app = FastAPI()

# Setup templates
templates = Jinja2Templates(directory="templates")

# Mount shared PHP admin JS components so Vue UI works out-of-the-box
app.mount("/js", StaticFiles(directory=os.path.join(os.path.dirname(__file__), '../repo-php/public/js')), name="js")

def get_admin_passkey():
    return os.environ.get('APP_ADMIN_PASSKEY') or os.environ.get('app_admin_passkey')

def verify_admin(request: Request):
    expected_passkey = get_admin_passkey()
    if not expected_passkey:
        raise HTTPException(status_code=403, detail="No passkey configured")
        
    passkey = request.headers.get("x-admin-passkey") or request.cookies.get("admin_passkey")
    if passkey != expected_passkey:
        raise HTTPException(status_code=403, detail="Unauthorized")
    return True

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

@app.get("/admin", response_class=HTMLResponse)
async def admin_app(request: Request):
    return templates.TemplateResponse(
        request=request,
        name="admin.html",
        context={}
    )

@app.get("/admin/api/forms")
async def admin_get_forms(site: str, request: Request, valid: bool = Depends(verify_admin)):
    return {"status": "success", "forms": []}

@app.post("/admin/api/forms/save")
async def admin_save_form(request: Request, valid: bool = Depends(verify_admin)):
    return {"status": "success"}

@app.post("/admin/api/forms/delete")
async def admin_delete_form(request: Request, valid: bool = Depends(verify_admin)):
    return {"status": "success"}

@app.get("/admin/api/forms/submissions")
async def admin_get_submissions(site: str, form_id: str, request: Request, valid: bool = Depends(verify_admin)):
    return {"status": "success", "submissions": []}

@app.get("/admin/api/sites")
async def admin_get_sites(request: Request, valid: bool = Depends(verify_admin)):
    return {"status": "success", "sites": [s.get("domain") for s in load_sites()]}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3000)
