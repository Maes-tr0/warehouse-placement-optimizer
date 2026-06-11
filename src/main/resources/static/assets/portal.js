"use strict";

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const page = document.body.dataset.page;
const role = document.body.dataset.role;
const state = {
    warehouseId: localStorage.getItem("wpo.activeWarehouseId") || "",
    warehouses: [],
    warehouse: null,
    articles: [],
    containers: [],
    places: [],
    assessment: null,
    plan: null,
    recommendation: null
};

const labels = {
    ACTIVE: "Активний", INACTIVE: "Неактивний", AVAILABLE: "Вільне", OCCUPIED: "Зайняте",
    WAITING_FOR_PLACEMENT: "Очікує розміщення", STORED: "Розміщена", MERGED: "Об'єднана", REMOVED: "Списана",
    SUGGESTED: "Запропоновано", ACCEPTED: "Підтверджено", REJECTED: "Відхилено", EXPIRED: "Протерміновано",
    HEALTHY: "Все добре", OPTIMIZATION_RECOMMENDED: "Потрібні перестановки", INSUFFICIENT_DATA: "Недостатньо даних",
    DRAFT: "Чернетка", APPROVED: "Затверджено", IN_PROGRESS: "Виконується", COMPLETED: "Завершено",
    CANCELLED: "Скасовано", PENDING: "Очікує", READY: "Готово", TRAINING: "Навчається", SUPERSEDED: "Замінено",
    ROOT_ADMIN: "Головний адміністратор", ADMIN: "Адміністратор", OPERATOR: "Оператор",
    PCS: "Штуки", BOX: "Коробки", PLACE: "Розмістити", MOVE: "Перемістити", TEMPORARY_MOVE: "Тимчасово перемістити",
    PUTAWAY: "Розміщення", RELOCATION: "Перестановка", TEMPORARY_RELOCATION: "Тимчасова перестановка",
    REMOVAL: "Списання", TRIBUO: "Машинне навчання", BASELINE: "Базовий розрахунок", SEASONAL: "Сезонний прогноз"
};

class ApiError extends Error {
    constructor(status, payload) {
        super(payload?.message || `HTTP ${status}`);
        this.status = status;
        this.payload = payload;
    }
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body !== undefined) headers.set("Content-Type", "application/json");
    const response = await fetch(path, {
        method: options.method || "GET",
        headers,
        credentials: "same-origin",
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
    const contentType = response.headers.get("content-type") || "";
    const payload = response.status === 204 ? null : contentType.includes("json") ? await response.json() : await response.text();
    if (response.status === 401) {
        location.assign("/login?expired");
        throw new ApiError(response.status, payload);
    }
    if (!response.ok) throw new ApiError(response.status, payload);
    return payload;
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"}[char]));
}

function label(value) {
    return labels[value] || value || "—";
}

function badge(value) {
    const good = ["ACTIVE", "AVAILABLE", "STORED", "ACCEPTED", "APPROVED", "HEALTHY", "COMPLETED"];
    const warn = ["WAITING_FOR_PLACEMENT", "SUGGESTED", "DRAFT", "READY", "IN_PROGRESS", "OPTIMIZATION_RECOMMENDED", "TRAINING", "PENDING"];
    const bad = ["INACTIVE", "REMOVED", "REJECTED", "CANCELLED", "EXPIRED", "FAILED"];
    const tone = good.includes(value) ? "good" : warn.includes(value) ? "warn" : bad.includes(value) ? "bad" : "info";
    return `<span class="badge ${tone}">${escapeHtml(label(value))}</span>`;
}

function formatDate(value) {
    if (!value) return "—";
    return new Intl.DateTimeFormat("uk-UA", {dateStyle: "medium", timeStyle: "short"}).format(new Date(value));
}

function num(value) {
    return value === "" || value == null ? null : Number(value);
}

function formData(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function toast(message, type = "success") {
    const root = $("#toastStack");
    if (!root) return;
    const item = document.createElement("div");
    item.className = `toast ${type}`;
    item.textContent = message;
    root.append(item);
    setTimeout(() => item.remove(), 4200);
}

function report(error) {
    console.error(error);
    toast(error.payload?.message || error.message || "Не вдалося виконати дію", "error");
}

function requireWarehouse() {
    if (!state.warehouseId) throw new Error("Спочатку виберіть активний склад");
    return state.warehouseId;
}

function setProcessStep(selector, activeIndex) {
    const steps = $$(`${selector} .process-step`);
    steps.forEach((step, index) => {
        step.classList.toggle("active", index === activeIndex);
        step.classList.toggle("done", index < activeIndex || activeIndex >= steps.length);
    });
}

function bindShell() {
    $("#menuButton")?.addEventListener("click", () => $("#sidebar")?.classList.toggle("open"));
    $("[data-refresh]")?.addEventListener("click", () => location.reload());
    $$('[data-dialog-open]').forEach(button => button.addEventListener("click", () => {
        const dialogId = button.dataset.dialogOpen;
        const dialog = $(`#${dialogId}`);
        if (dialogId === "articleDialog") {
            const form = $("#articleForm");
            form.reset();
            form.elements.id.value = "";
            form.elements.articleNumber.disabled = false;
            $("#articleDialogTitle").textContent = "Новий товар";
        }
        if (dialogId === "accountDialog") {
            const form = $("#accountForm");
            form.reset();
            form.elements.id.value = "";
            form.elements.password.required = true;
            $("#accountDialogTitle").textContent = "Новий користувач";
        }
        dialog?.showModal();
    }));
    $$('[data-dialog-close]').forEach(button => button.addEventListener("click", () => button.closest("dialog")?.close()));
    $$(".tab").forEach(tab => tab.addEventListener("click", () => {
        $$(".tab").forEach(item => item.classList.toggle("active", item === tab));
        $$(".tab-panel").forEach(panel => panel.classList.toggle("active", panel.id === `tab-${tab.dataset.tab}`));
    }));
}

async function loadWarehouses() {
    const picker = $("#warehouseSelect");
    if (!picker) return;
    state.warehouses = await api(role === "operator" ? "/operator/warehouses" : "/admin/warehouses");
    if (!state.warehouseId && state.warehouses.length) state.warehouseId = String(state.warehouses[0].id);
    if (state.warehouseId && !state.warehouses.some(item => String(item.id) === String(state.warehouseId))) {
        state.warehouseId = state.warehouses.length ? String(state.warehouses[0].id) : "";
    }
    picker.innerHTML = state.warehouses.length
        ? state.warehouses.map(item => `<option value="${item.id}">${escapeHtml(item.warehouseCode)} · ${escapeHtml(item.warehouseName)}</option>`).join("")
        : '<option value="">Складів немає</option>';
    picker.value = state.warehouseId;
    localStorage.setItem("wpo.activeWarehouseId", state.warehouseId);
    picker.addEventListener("change", event => {
        localStorage.setItem("wpo.activeWarehouseId", event.target.value);
        location.reload();
    });
}

async function loadAdminDashboard() {
    const warehouseId = requireWarehouse();
    const results = await Promise.allSettled([
        api(`/admin/warehouses/${warehouseId}`), api(`/admin/warehouses/${warehouseId}/storage-places`),
        api("/operator/containers"), api("/admin/articles"),
        api(`/admin/warehouses/${warehouseId}/optimization-assessments/latest`),
        api(`/admin/warehouses/${warehouseId}/demand-forecast-models/latest`)
    ]);
    const value = index => results[index].status === "fulfilled" ? results[index].value : null;
    const warehouse = value(0), places = value(1) || [], containers = (value(2) || []).filter(item => String(item.warehouseId) === String(warehouseId));
    const articles = value(3) || [], assessment = value(4), model = value(5);
    $("#heroWarehouseName").textContent = warehouse?.warehouseName || "Оберіть склад";
    $("#metricPlaces").textContent = places.length;
    $("#metricAvailable").textContent = `${places.filter(item => item.status === "AVAILABLE").length} вільно`;
    $("#metricContainers").textContent = containers.length;
    $("#metricWaiting").textContent = `${containers.filter(item => item.status === "WAITING_FOR_PLACEMENT").length} очікують`;
    $("#metricArticles").textContent = articles.length;
    if (model) { $("#metricModel").textContent = `v${model.versionNumber}`; $("#metricModelDate").textContent = formatDate(model.trainedAt); }
    if (assessment) {
        const score = Number(assessment.scorePercent || 0);
        $("#dashboardScore").style.setProperty("--score", score);
        $("#scoreValue").textContent = `${score.toFixed(1)}%`;
    }
    const waiting = containers.filter(item => item.status === "WAITING_FOR_PLACEMENT").length;
    const attention = [
        {title: `${waiting} палет очікують розміщення`, text: waiting ? "Операторам потрібно завершити приймання." : "Черга на розміщення порожня.", href: "/app/operator/placement"},
        {title: assessment?.optimizationRecommended ? "Склад потребує оптимізації" : "Критичних перестановок не потрібно", text: assessment ? `Остання оцінка: ${assessment.scorePercent}%` : "Оцінка ще не виконувалася.", href: "/app/admin/optimization"},
        {title: model ? `Активна модель попиту v${model.versionNumber}` : "Модель попиту ще не навчена", text: model ? formatDate(model.trainedAt) : "Імпортуйте історію замовлень.", href: "/app/admin/demand"}
    ];
    $("#adminAttention").classList.remove("loading-state");
    $("#adminAttention").innerHTML = attention.map(item => `<a class="task-item" href="${item.href}"><div><b>${escapeHtml(item.title)}</b><p>${escapeHtml(item.text)}</p></div><span>→</span></a>`).join("");
}

async function loadWarehousePage() {
    const warehouseId = requireWarehouse();
    state.warehouse = await api(`/admin/warehouses/${warehouseId}`);
    $("#warehouseAisles").textContent = state.warehouse.aisleCount ?? "—";
    $("#warehouseRows").textContent = state.warehouse.rackRowCount ?? "—";
    $("#warehousePlaces").textContent = state.warehouse.storagePlaceCount ?? "—";
    $("#warehouseStatus").innerHTML = badge(state.warehouse.status);
    await loadPlaces();
    $("[data-load-places]").addEventListener("click", () => loadPlaces().catch(report));
    $("#placeSearch").addEventListener("input", renderPlaces);
    $("#placeStatus").addEventListener("change", () => loadPlaces().catch(report));
    $("#routeForm").addEventListener("submit", event => { event.preventDefault(); loadRoute(formData(event.currentTarget).storagePlaceCode).catch(report); });
    $("#warehouseForm").addEventListener("submit", createWarehouse);
}

async function loadPlaces() {
    const status = $("#placeStatus")?.value || "";
    state.places = await api(`/admin/warehouses/${requireWarehouse()}/storage-places${status ? `?status=${status}` : ""}`);
    renderPlaces();
}

function renderPlaces() {
    const query = $("#placeSearch")?.value.trim().toLowerCase() || "";
    const items = state.places.filter(item => !query || item.code.toLowerCase().includes(query));
    $("#placesTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.code)}</b></td><td>${escapeHtml(item.rackRowCode)} / L${item.levelNumber}</td><td>${(item.distanceFromEntryMm / 1000).toFixed(1)} м</td><td>${item.maxWeightKg} кг · ${item.maxHeightMm} мм</td><td>${badge(item.status)}</td><td><button class="mini-button" data-route="${escapeHtml(item.code)}">Маршрут</button></td></tr>`).join("") : '<tr><td colspan="6" class="empty-cell">Локацій не знайдено</td></tr>';
}

async function loadRoute(code) {
    const route = await api(`/admin/warehouses/${requireWarehouse()}/routes/storage-places/${encodeURIComponent(code)}`);
    $("#routeForm [name=storagePlaceCode]").value = code;
    $("#routeSummary").innerHTML = `<b>${escapeHtml(route.storagePlaceCode)}</b> · ${(route.distanceMm / 1000).toFixed(1)} м · приблизно ${route.estimatedTravelTimeSeconds} с`;
    const svg = $("#routeMap");
    if (!route.points?.length) { svg.innerHTML = ""; return; }
    const xs = route.points.map(point => point.xMm), ys = route.points.map(point => point.yMm);
    const minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys);
    const scaleX = x => 35 + (x - minX) / Math.max(maxX - minX, 1) * 650;
    const scaleY = y => 35 + (y - minY) / Math.max(maxY - minY, 1) * 190;
    const points = route.points.map(point => `${scaleX(point.xMm)},${scaleY(point.yMm)}`).join(" ");
    svg.innerHTML = `<polyline points="${points}" fill="none" stroke="#17231e" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>${route.points.map((point, index) => `<circle cx="${scaleX(point.xMm)}" cy="${scaleY(point.yMm)}" r="${index === route.points.length - 1 ? 10 : 6}" fill="${index === route.points.length - 1 ? "#d9ff63" : "#57d39b"}" stroke="#17231e" stroke-width="3"/>`).join("")}`;
}

async function createWarehouse(event) {
    event.preventDefault();
    const data = formData(event.currentTarget);
    const levelProfiles = [1, 2, 3].map(level => ({levelNumber: level, clearHeightMm: num(data[`level${level}Height`]), maxCellLoadKg: num(data[`level${level}Weight`])}));
    [1, 2, 3].forEach(level => { delete data[`level${level}Height`]; delete data[`level${level}Weight`]; });
    try {
        const created = await api("/admin/warehouses", {method: "POST", body: {...data, aisleCount:num(data.aisleCount), rackRowCount:num(data.rackRowCount), baysPerRackRow:num(data.baysPerRackRow), palletPlacesPerLevel:num(data.palletPlacesPerLevel), aisleWidthMm:num(data.aisleWidthMm), maxBayLoadKg:num(data.maxBayLoadKg), levelProfiles}});
        localStorage.setItem("wpo.activeWarehouseId", created.id); toast("Склад створено"); location.reload();
    } catch (error) { report(error); }
}

async function loadArticles() {
    state.articles = await api("/admin/articles");
    $("#articlesTable").innerHTML = state.articles.length ? state.articles.map(item => `<tr><td><b>${escapeHtml(item.articleNumber)}</b></td><td>${escapeHtml(item.name)}</td><td>${escapeHtml(label(item.unitType))}</td><td>${item.unitWidthMm}×${item.unitLengthMm}×${item.unitHeightMm} мм</td><td>${item.unitWeightKg} кг</td><td>${item.maxQuantityPerPallet}</td><td><div class="row-actions"><button class="mini-button" data-article-edit="${item.id}">Змінити</button><button class="mini-button danger" data-article-delete="${item.id}">Видалити</button></div></td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Товарів ще немає</td></tr>';
}

async function loadContainers() {
    state.containers = await api("/operator/containers");
    renderContainers();
}

function renderContainers() {
    const query = $("#containerSearch")?.value.trim().toLowerCase() || "";
    const status = $("#containerStatus")?.value || "";
    const items = state.containers.filter(item => (!state.warehouseId || String(item.warehouseId) === String(state.warehouseId)) && (!status || item.status === status) && (!query || `${item.containerNumber} ${item.articleNumber} ${item.articleName}`.toLowerCase().includes(query)));
    const actions = page === "operator-inventory";
    $("#containersTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.containerNumber)}</b></td><td>${escapeHtml(item.articleNumber)}<small class="block">${escapeHtml(item.articleName)}</small></td><td>${item.quantity}</td><td>${item.weightKg} кг · ${item.heightMm} мм</td><td>${escapeHtml(item.currentStoragePlaceCode || "—")}</td><td>${badge(item.status)}</td>${actions ? `<td><div class="row-actions"><button class="mini-button" data-container-edit="${escapeHtml(item.containerNumber)}">Змінити</button><button class="mini-button danger" data-container-remove="${escapeHtml(item.containerNumber)}">Списати</button></div></td>` : ""}</tr>`).join("") : `<tr><td colspan="${actions ? 7 : 6}" class="empty-cell">Палет не знайдено</td></tr>`;
}

async function loadAdminInventory() {
    await Promise.all([loadArticles(), loadContainers()]);
    $("#containerSearch").addEventListener("input", renderContainers);
    $("[data-load-containers]").addEventListener("click", () => loadContainers().catch(report));
    $("#articleForm").addEventListener("submit", saveArticle);
}

async function saveArticle(event) {
    event.preventDefault(); const data = formData(event.currentTarget), id = data.id; delete data.id;
    if (id) delete data.articleNumber;
    ["unitWidthMm", "unitLengthMm", "unitHeightMm", "unitWeightKg", "maxQuantityPerPallet"].forEach(key => data[key] = num(data[key]));
    try { await api(id ? `/admin/articles/${id}` : "/admin/articles", {method: id ? "PATCH" : "POST", body: data}); $("#articleDialog").close(); event.currentTarget.reset(); await loadArticles(); toast("Товар збережено"); } catch (error) { report(error); }
}

function renderModel(model) {
    $("#modelVersion").textContent = `v${model.versionNumber} · ${label(model.status)}`;
    $("#modelAlgorithm").textContent = model.algorithm?.toLowerCase().includes("tribuo") ? "Tribuo ML" : model.algorithm || "—";
    $("#modelImprovement").textContent = model.improvementPercent == null ? "—" : `${model.improvementPercent.toFixed(1)}%`;
    $("#modelObservations").textContent = model.observationCount;
}

async function loadDemand() {
    const data = formData($("#demandFilterForm")); const params = new URLSearchParams();
    if (data.from) params.set("from", data.from); if (data.to) params.set("to", data.to);
    const items = await api(`/admin/warehouses/${requireWarehouse()}/demand/articles${params.size ? `?${params}` : ""}`);
    $("#demandTable").innerHTML = items.length ? items.slice(0, Number(data.limit || 30)).map(item => `<tr><td>#${item.rank}</td><td><b>${escapeHtml(item.articleNumber)}</b><small class="block">${escapeHtml(item.articleName)}</small></td><td>${item.demandScore.toFixed(3)}</td><td>${item.historicalQuantity} / ${item.orderCount} зам.</td><td>${item.storedQuantity} · ${item.storedContainerCount} палет</td><td>${item.averageDistanceFromEntryMm ? `${(item.averageDistanceFromEntryMm / 1000).toFixed(1)} м` : "—"}</td><td>${badge(item.scoreSource)}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Даних попиту немає</td></tr>';
}

async function loadModels() {
    const items = await api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models`);
    $("#modelsTable").innerHTML = items.length ? items.map(item => `<tr><td><b>v${item.versionNumber}</b><small class="block">${escapeHtml(item.code)}</small></td><td>${badge(item.status)}</td><td>${item.trainingStart} → ${item.trainingEnd}</td><td>${item.modelMae?.toFixed(3) ?? "—"}</td><td>${item.baselineMae?.toFixed(3) ?? "—"}</td><td>${item.improvementPercent?.toFixed(1) ?? "—"}%</td><td>${formatDate(item.trainedAt)}</td></tr>`).join("") : '<tr><td colspan="7" class="empty-cell">Моделей ще немає</td></tr>';
}

async function loadAdminDemand() {
    const results = await Promise.allSettled([loadDemand(), api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models/latest`), loadModels()]);
    if (results[1].status === "fulfilled") renderModel(results[1].value);
    $("#demandFilterForm").addEventListener("submit", event => { event.preventDefault(); loadDemand().catch(report); });
    $("#trainModelButton").addEventListener("click", async () => { try { const model = await api(`/admin/warehouses/${requireWarehouse()}/demand-forecast-models/train`, {method:"POST"}); renderModel(model); await loadModels(); toast("Модель навчено"); } catch (error) { report(error); } });
    $("#demandImportForm").addEventListener("submit", async event => { event.preventDefault(); try { const result = await api("/admin/demand-history/import", {method:"POST", body:{warehouseId:num(requireWarehouse()), orders:JSON.parse(formData(event.currentTarget).orders)}}); $("#demandDialog").close(); await loadDemand(); toast(`Імпортовано замовлень: ${result.importedOrders}`); } catch (error) { report(error); } });
}

function renderAssessment(item) {
    state.assessment = item; const score = Number(item.scorePercent || 0);
    $("#optimizationRing").style.setProperty("--score", score); $("#optimizationScore").textContent = `${score.toFixed(1)}%`;
    $("#optimizationHeadline").textContent = item.optimizationRecommended ? "Потрібен план перестановок" : item.status === "INSUFFICIENT_DATA" ? "Недостатньо даних" : "Склад оптимізований достатньо";
    $("#optimizationDetails").textContent = `Поріг ${item.thresholdPercent}%. Проаналізовано ${item.analyzedContainerCount} палет і ${item.demandObservationCount} записів попиту.`;
    $("#assessmentDetails").innerHTML = `<div><dt>Статус</dt><dd>${label(item.status)}</dd></div><div><dt>Палети з попитом</dt><dd>${item.demandMatchedContainerCount}/${item.analyzedContainerCount}</dd></div><div><dt>Середня відстань</dt><dd>${item.weightedAverageDistanceMm ? `${(item.weightedAverageDistanceMm / 1000).toFixed(1)} м` : "—"}</dd></div><div><dt>Перевірено</dt><dd>${formatDate(item.analyzedAt)}</dd></div>`;
    $("#createPlanButton").disabled = !item.optimizationRecommended;
    setProcessStep(".optimization-steps", item.optimizationRecommended ? 1 : 0);
}

function renderPlan(plan) {
    state.plan = plan;
    $("#planQuickState").innerHTML = `<b>${escapeHtml(plan.code)}</b> · ${label(plan.status)} · ${plan.completedSteps}/${plan.totalSteps} кроків`;
    $("#planCard").innerHTML = `<div class="section-head"><div><p class="eyebrow">${escapeHtml(plan.code)}</p><h2>План перестановок</h2></div>${badge(plan.status)}</div><div class="metric-grid compact"><article class="metric"><span>Було</span><b>${plan.initialScorePercent}%</b></article><article class="metric"><span>Буде</span><b>${plan.projectedScorePercent}%</b></article><article class="metric"><span>Ціль</span><b>${plan.targetScorePercent}%</b></article><article class="metric"><span>Економія</span><b>${Math.round(plan.estimatedTimeSavingSeconds / 60)} хв</b></article></div><div class="button-row"><button class="button primary" data-plan-approve="${escapeHtml(plan.code)}" ${plan.status !== "DRAFT" ? "disabled" : ""}>Затвердити для оператора</button><button class="button ghost" data-plan-cancel="${escapeHtml(plan.code)}" ${["COMPLETED","CANCELLED"].includes(plan.status) ? "disabled" : ""}>Скасувати</button><a class="button subtle" href="/app/operator/relocation?plan=${encodeURIComponent(plan.code)}">Відкрити виконання</a></div><div class="step-list">${(plan.steps || []).map(step => `<div class="step-item"><span class="step-number">${step.sequenceNumber}</span><div><b>${escapeHtml(label(step.type))} · ${escapeHtml(step.sourceContainerNumber)}</b><small>${escapeHtml(step.fromStoragePlaceCode || "—")} → ${escapeHtml(step.toStoragePlaceCode || step.targetContainerNumber || "—")}</small></div>${badge(step.status)}</div>`).join("")}</div>`;
    setProcessStep(".optimization-steps", plan.status === "DRAFT" ? 2 : ["APPROVED","IN_PROGRESS"].includes(plan.status) ? 3 : plan.status === "COMPLETED" ? 4 : 0);
}

async function loadAdminOptimization() {
    try { renderAssessment(await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments/latest`)); } catch (error) { if (error.status !== 404) report(error); }
    $("#runAssessmentButton").addEventListener("click", async () => { try { renderAssessment(await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments`, {method:"POST"})); toast("Оцінку завершено"); } catch (error) { report(error); } });
    $("#latestAssessmentButton").addEventListener("click", async () => { try { renderAssessment(await api(`/admin/warehouses/${requireWarehouse()}/optimization-assessments/latest`)); } catch (error) { report(error); } });
    $("#createPlanButton").addEventListener("click", async () => { try { renderPlan(await api(`/admin/optimization-plans/assessments/${state.assessment.id}`, {method:"POST"})); toast("План створено"); } catch (error) { report(error); } });
    $("#planLookupForm").addEventListener("submit", async event => { event.preventDefault(); try { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(formData(event.currentTarget).planCode)}`)); } catch (error) { report(error); } });
}

async function loadAccounts() {
    const items = await api("/admin/accounts");
    $("#accountsTable").innerHTML = items.length ? items.map(item => `<tr><td><b>${escapeHtml(item.fullName)}</b><small class="block">${escapeHtml(item.email)}</small></td><td>${badge(item.role)}</td><td>${badge(item.status)}</td><td>${formatDate(item.createdAt)}</td><td>${formatDate(item.updatedAt)}</td><td><div class="row-actions"><button class="mini-button" data-account-edit="${item.id}">Змінити</button><button class="mini-button" data-account-toggle="${item.id}" data-status="${item.status}">${item.status === "ACTIVE" ? "Заблокувати" : "Активувати"}</button></div></td></tr>`).join("") : '<tr><td colspan="6" class="empty-cell">Користувачів немає</td></tr>';
}

async function loadAdminAccounts() {
    await loadAccounts();
    $("#accountForm").addEventListener("submit", async event => { event.preventDefault(); const data = formData(event.currentTarget), id = data.id; delete data.id; if (id && !data.password) delete data.password; try { await api(id ? `/admin/accounts/${id}` : "/admin/accounts", {method:id ? "PATCH" : "POST", body:data}); $("#accountDialog").close(); event.currentTarget.reset(); await loadAccounts(); toast("Користувача збережено"); } catch (error) { report(error); } });
}

async function loadOperatorDashboard() {
    await loadContainers();
    const items = state.containers.filter(item => !state.warehouseId || String(item.warehouseId) === String(state.warehouseId));
    $("#operatorContainers").textContent = items.length;
    $("#operatorWaiting").textContent = items.filter(item => item.status === "WAITING_FOR_PLACEMENT").length;
    $("#operatorStored").textContent = items.filter(item => item.status === "STORED").length;
    $("#operatorClosed").textContent = items.filter(item => ["MERGED","REMOVED"].includes(item.status)).length;
}

async function loadOperatorReceiving() {
    $("#receiveForm").addEventListener("submit", async event => { event.preventDefault(); const data = formData(event.currentTarget); try { const item = await api("/operator/containers/receive", {method:"POST", body:{warehouseId:num(requireWarehouse()), containerNumber:data.containerNumber, articleNumber:data.articleNumber, quantity:num(data.quantity), weightKg:num(data.weightKg), heightMm:num(data.heightMm)}}); $("#receivingResult").className = "result-note"; $("#receivingResult").innerHTML = `<b>${escapeHtml(item.containerNumber)}</b> · ${escapeHtml(item.articleNumber)} · ${item.quantity} од. ${badge(item.status)}`; event.currentTarget.reset(); setProcessStep(".process-steps", 3); toast("Палету прийнято"); } catch (error) { report(error); } });
    $("#receiveBatchForm").addEventListener("submit", async event => { event.preventDefault(); const rows = formData(event.currentTarget).rows.split(/\r?\n/).filter(Boolean).map(row => { const [containerNumber,articleNumber,quantity,weightKg,heightMm] = row.split(",").map(value => value.trim()); return {warehouseId:num(requireWarehouse()),containerNumber,articleNumber,quantity:num(quantity),weightKg:num(weightKg),heightMm:num(heightMm)}; }); try { const result = await api("/operator/containers/receive/batch", {method:"POST", body:{containers:rows}}); $("#receivingResult").className = "result-note"; $("#receivingResult").textContent = `Прийнято палет: ${result.receivedContainers}`; toast("Пакетне приймання завершено"); } catch (error) { report(error); } });
}

function renderRecommendation(item) {
    state.recommendation = item;
    const target = item.recommendationType === "MERGE" ? item.targetContainerNumber : item.recommendedStoragePlaceCode;
    const scanTarget = item.recommendedStoragePlaceCode || item.targetStoragePlaceCode || "";
    const closed = ["ACCEPTED","REJECTED","EXPIRED"].includes(item.status);
    $("#recommendationCard").innerHTML = `<div class="recommendation-result"><div><p class="eyebrow">РЕКОМЕНДОВАНА ДІЯ · ${escapeHtml(label(item.recommendationType))}</p><div class="recommendation-target">${escapeHtml(target)}</div><div class="recommendation-meta">${badge(item.status)}${item.distanceFromEntryMm != null ? `<span class="badge info">${(item.distanceFromEntryMm/1000).toFixed(1)} м від входу</span>` : ""}${item.estimatedTimeSeconds != null ? `<span class="badge info">${item.estimatedTimeSeconds} с</span>` : ""}</div><p>${escapeHtml(item.reason)}</p></div>${closed ? `<div class="success-state"><b>${item.status === "ACCEPTED" ? "Дію підтверджено" : "Рекомендацію закрито"}</b><p>Можна сканувати наступну палету.</p></div>` : `<div class="scan-confirmation"><label>Відскануйте цільову локацію<input id="targetScan" value="${escapeHtml(scanTarget)}" required></label><button class="button accent wide" data-recommend-approve="${escapeHtml(item.code)}">Підтвердити</button><button class="button ghost wide" data-recommend-reject="${escapeHtml(item.code)}">Відхилити</button></div>`}</div>`;
    setProcessStep(".placement-steps", item.status === "ACCEPTED" ? 3 : closed ? 0 : 2);
}

async function loadOperatorPlacement() {
    $("#recommendForm").addEventListener("submit", async event => { event.preventDefault(); setProcessStep(".placement-steps", 1); try { renderRecommendation(await api("/operator/placement/recommend", {method:"POST", body:formData(event.currentTarget)})); } catch (error) { setProcessStep(".placement-steps", 0); report(error); } });
}

function renderCurrentStep(step) {
    $("#currentStepCard").className = "result-note";
    $("#currentStepCard").innerHTML = `<p class="eyebrow">ПОТОЧНИЙ КРОК ${step.sequenceNumber}</p><h2>${escapeHtml(label(step.type))}</h2><dl class="detail-list compact"><div><dt>Палета</dt><dd>${escapeHtml(step.sourceContainerNumber)}</dd></div><div><dt>Звідки</dt><dd>${escapeHtml(step.fromStoragePlaceCode || "—")}</dd></div><div><dt>Куди</dt><dd>${escapeHtml(step.toStoragePlaceCode || step.targetContainerNumber || "—")}</dd></div></dl><p>${escapeHtml(step.reason)}</p>${badge(step.status)}`;
    const form = $("#completeStepForm"); form.elements.sourceContainerNumber.value = step.sourceContainerNumber || ""; form.elements.targetStoragePlaceCode.value = step.toStoragePlaceCode || ""; form.elements.targetContainerNumber.value = step.targetContainerNumber || "";
}

async function loadOperatorRelocation() {
    const initialPlan = new URLSearchParams(location.search).get("plan"); if (initialPlan) $("#currentStepForm [name=planCode]").value = initialPlan;
    $("#currentStepForm").addEventListener("submit", async event => { event.preventDefault(); const code = formData(event.currentTarget).planCode; try { renderCurrentStep(await api(`/operator/optimization-plans/${encodeURIComponent(code)}/steps/current`)); $("#completeStepForm [name=planCode]").value = code; } catch (error) { report(error); } });
    $("#completeStepForm").addEventListener("submit", async event => { event.preventDefault(); const data = formData(event.currentTarget); try { const result = await api(`/operator/optimization-plans/${encodeURIComponent(data.planCode)}/steps/current/complete`, {method:"POST", body:{sourceContainerNumber:data.sourceContainerNumber, targetStoragePlaceCode:data.targetStoragePlaceCode, targetContainerNumber:data.targetContainerNumber || null}}); if (result.nextStep) renderCurrentStep(result.nextStep); else $("#currentStepCard").innerHTML = '<div class="success-state"><b>План виконано</b><p>Усі перестановки підтверджені.</p></div>'; toast("Переміщення підтверджено"); } catch (error) { report(error); } });
}

async function loadOperatorInventory() {
    await loadContainers();
    $("#containerSearch").addEventListener("input", renderContainers); $("#containerStatus").addEventListener("change", renderContainers); $("[data-load-containers]").addEventListener("click", () => loadContainers().catch(report));
    $("#containerForm").addEventListener("submit", async event => { event.preventDefault(); const data = formData(event.currentTarget); try { await api(`/operator/containers/${encodeURIComponent(data.containerNumber)}`, {method:"PATCH", body:{quantity:num(data.quantity), weightKg:num(data.weightKg), heightMm:num(data.heightMm)}}); $("#containerDialog").close(); await loadContainers(); toast("Дані палети оновлено"); } catch (error) { report(error); } });
}

function bindDelegatedActions() {
    document.addEventListener("click", async event => {
        const button = event.target.closest("button"); if (!button) return;
        try {
            if (button.dataset.route) await loadRoute(button.dataset.route);
            if (button.dataset.articleEdit) { const item = await api(`/admin/articles/${button.dataset.articleEdit}`); const form = $("#articleForm"); Object.entries(item).forEach(([key,value]) => { if (form.elements[key]) form.elements[key].value = value ?? ""; }); form.elements.id.value = item.id; form.elements.articleNumber.disabled = true; $("#articleDialogTitle").textContent = `Редагування ${item.articleNumber}`; $("#articleDialog").showModal(); }
            if (button.dataset.articleDelete && confirm("Видалити товар з довідника?")) { await api(`/admin/articles/${button.dataset.articleDelete}`, {method:"DELETE"}); await loadArticles(); toast("Товар видалено"); }
            if (button.dataset.accountEdit) { const item = await api(`/admin/accounts/${button.dataset.accountEdit}`); const form = $("#accountForm"); Object.entries(item).forEach(([key,value]) => { if (form.elements[key]) form.elements[key].value = value ?? ""; }); form.elements.id.value = item.id; form.elements.password.required = false; $("#accountDialogTitle").textContent = `Редагування ${item.fullName}`; $("#accountDialog").showModal(); }
            if (button.dataset.accountToggle) { const action = button.dataset.status === "ACTIVE" ? "deactivate" : "activate"; await api(`/admin/accounts/${button.dataset.accountToggle}/${action}`, {method:"PATCH"}); await loadAccounts(); toast("Статус користувача змінено"); }
            if (button.dataset.planApprove) { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(button.dataset.planApprove)}/approve`, {method:"POST"})); toast("План передано оператору"); }
            if (button.dataset.planCancel) { renderPlan(await api(`/admin/optimization-plans/${encodeURIComponent(button.dataset.planCancel)}/cancel`, {method:"POST"})); toast("План скасовано"); }
            if (button.dataset.recommendApprove) { const scanned = $("#targetScan")?.value.trim(); if (!scanned) { toast("Відскануйте цільову локацію", "error"); return; } renderRecommendation(await api(`/operator/placement/recommendations/${encodeURIComponent(button.dataset.recommendApprove)}/approve`, {method:"POST", body:{scannedStoragePlaceCode:scanned}})); toast("Розміщення підтверджено"); }
            if (button.dataset.recommendReject) { renderRecommendation(await api(`/operator/placement/recommendations/${encodeURIComponent(button.dataset.recommendReject)}/reject`, {method:"POST"})); toast("Рекомендацію відхилено"); }
            if (button.dataset.containerEdit) { const item = await api(`/operator/containers/${encodeURIComponent(button.dataset.containerEdit)}`); const form = $("#containerForm"); form.elements.containerNumber.value = item.containerNumber; form.elements.quantity.value = item.quantity; form.elements.weightKg.value = item.weightKg; form.elements.heightMm.value = item.heightMm; $("#containerDialogTitle").textContent = `Палета ${item.containerNumber}`; $("#containerDialog").showModal(); }
            if (button.dataset.containerRemove && confirm("Списати цю палету?")) { await api(`/operator/containers/${encodeURIComponent(button.dataset.containerRemove)}/remove`, {method:"PATCH"}); await loadContainers(); toast("Палету списано"); }
        } catch (error) { report(error); }
    });
}

const pageInitializers = {
    "admin-dashboard": loadAdminDashboard,
    "admin-warehouse": loadWarehousePage,
    "admin-inventory": loadAdminInventory,
    "admin-demand": loadAdminDemand,
    "admin-optimization": loadAdminOptimization,
    "admin-accounts": loadAdminAccounts,
    "operator-dashboard": loadOperatorDashboard,
    "operator-receiving": loadOperatorReceiving,
    "operator-placement": loadOperatorPlacement,
    "operator-relocation": loadOperatorRelocation,
    "operator-inventory": loadOperatorInventory
};

async function init() {
    bindShell(); bindDelegatedActions();
    try {
        await loadWarehouses();
        await pageInitializers[page]?.();
    } catch (error) { report(error); }
}

document.addEventListener("DOMContentLoaded", init);
