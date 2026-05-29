use actix_web::{get, post, App, HttpResponse, HttpServer, Responder, HttpRequest};
use actix_files::Files;
use actix_files::NamedFile;
use serde::Serialize;
use serde::Deserialize;
use std::env;
use std::fs;
use std::path::PathBuf;

#[derive(Serialize)]
struct StatusResponse {
    status: String,
}

#[derive(Serialize)]
struct SitesResponse {
    status: String,
    sites: Vec<String>,
}

#[derive(Serialize)]
struct FormsResponse {
    status: String,
    forms: Vec<String>,
}

#[derive(Serialize)]
struct SubmissionsResponse {
    status: String,
    submissions: Vec<String>,
}

#[derive(Serialize)]
struct TasksResponse {
    status: String,
    tasks: Vec<String>,
}

fn verify_admin(req: &HttpRequest) -> bool {
    let expected_passkey = env::var("APP_ADMIN_PASSKEY").unwrap_or_else(|_| env::var("app_admin_passkey").unwrap_or_default());
    if expected_passkey.is_empty() { return false; }
    
    let mut passkey = String::new();
    if let Some(val) = req.headers().get("X-Admin-Passkey") {
        passkey = val.to_str().unwrap_or("").to_string();
    } else if let Some(val) = req.headers().get("x-admin-passkey") {
        passkey = val.to_str().unwrap_or("").to_string();
    } else if let Some(cookie) = req.cookie("admin_passkey") {
        passkey = cookie.value().to_string();
    }

    passkey == expected_passkey
}

#[get("/")]
async fn hello() -> impl Responder {
    HttpResponse::Ok().body("Hello from Rust!")
}

#[get("/admin")]
async fn admin() -> impl Responder {
    NamedFile::open_async("./src/admin.html").await
}

#[get("/admin/api/sites")]
async fn api_sites(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    
    let mut sites = Vec::new();
    if let Ok(entries) = fs::read_dir("../content") {
        for entry in entries.flatten() {
            if entry.file_type().map(|ft| ft.is_dir()).unwrap_or(false) {
                if let Ok(name) = entry.file_name().into_string() {
                    sites.push(name);
                }
            }
        }
    }
    HttpResponse::Ok().json(SitesResponse { status: "success".to_string(), sites })
}

#[get("/admin/api/forms")]
async fn api_forms(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(FormsResponse { status: "success".to_string(), forms: vec![] })
}

#[post("/admin/api/forms/save")]
async fn api_forms_save(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(StatusResponse { status: "success".to_string() })
}

#[post("/admin/api/forms/delete")]
async fn api_forms_delete(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(StatusResponse { status: "success".to_string() })
}

#[get("/admin/api/forms/submissions")]
async fn api_forms_submissions(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(SubmissionsResponse { status: "success".to_string(), submissions: vec![] })
}

#[get("/admin/api/ai/tasks")]
async fn api_ai_tasks(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(TasksResponse { status: "success".to_string(), tasks: vec![] })
}

#[post("/admin/api/ai/tasks/add")]
async fn api_ai_tasks_add(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(StatusResponse { status: "success".to_string() })
}

#[get("/admin/api/ai/heartbeat")]
async fn api_ai_heartbeat(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(StatusResponse { status: "success".to_string() })
}

#[post("/admin/api/sync")]
async fn api_sync(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(StatusResponse { status: "success".to_string() })
}

#[get("/admin/api/sites/{site}/download")]
async fn api_sites_download(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(StatusResponse { status: "success".to_string() })
}

#[post("/admin/api/sites/{site}/upload")]
async fn api_sites_upload(req: HttpRequest) -> impl Responder {
    if !verify_admin(&req) { return HttpResponse::Forbidden().json(StatusResponse { status: "Unauthorized".to_string() }); }
    HttpResponse::Ok().json(StatusResponse { status: "success".to_string() })
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    HttpServer::new(|| {
        App::new()
            .service(Files::new("/js", "../repo-php/public/js").show_files_listing())
            .service(hello)
            .service(admin)
            .service(api_sites)
            .service(api_forms)
            .service(api_forms_save)
            .service(api_forms_delete)
            .service(api_forms_submissions)
            .service(api_ai_tasks)
            .service(api_ai_tasks_add)
            .service(api_ai_heartbeat)
            .service(api_sync)
            .service(api_sites_download)
            .service(api_sites_upload)
    })
    .bind(("0.0.0.0", 8081))?
    .run()
    .await
}
