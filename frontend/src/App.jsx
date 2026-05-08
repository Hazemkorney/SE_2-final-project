import React, { useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import { Button, Card, CardBody, CardHeader, Chip, Divider, Input, Select, SelectItem } from "@heroui/react";

const API = import.meta.env.VITE_API_URL || "http://localhost:18080";

const shellStyle = { maxWidth: 1140, margin: "24px auto", fontFamily: "Inter, Arial, sans-serif", padding: "0 14px" };
const cardStyle = {
  border: "1px solid #e2e8f0",
  borderRadius: 14,
  padding: 18,
  marginBottom: 14,
  background: "#ffffff",
  boxShadow: "0 12px 28px rgba(15, 23, 42, 0.08)"
};

async function apiRequest(path, method = "GET", body = null, token = "") {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${API}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : null
  });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      message = await res.text();
    } catch (e) {
      // ignore parse failures and keep fallback message
    }
    throw new Error(message || `Request failed (${res.status})`);
  }
  if (res.status === 204) return null;
  return res.json();
}

function LoginView({ onLogin }) {
  const [email, setEmail] = useState("admin@hospital.com");
  const [password, setPassword] = useState("admin123");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async () => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setError("");
    try {
      const data = await apiRequest("/auth/login", "POST", { email, password });
      onLogin(data.token, data.role);
    } catch (e) {
      setError(e.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 px-4 py-10">
      <div className="pointer-events-none absolute inset-0 opacity-60">
        <div className="absolute -left-24 top-[-120px] h-80 w-80 rounded-full bg-fuchsia-500/30 blur-3xl" />
        <div className="absolute right-[-90px] top-12 h-72 w-72 rounded-full bg-sky-500/35 blur-3xl" />
        <div className="absolute bottom-[-140px] left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-emerald-400/20 blur-3xl" />
      </div>
      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="relative w-full max-w-md"
      >
        <Card className="border border-white/15 bg-white/95 shadow-2xl backdrop-blur-xl dark:bg-slate-900/85" shadow="lg" radius="lg">
          <CardHeader className="flex flex-col gap-3 pb-3">
            <div className="flex w-full items-center justify-between">
              <div>
                <h2 className="m-0 text-3xl font-bold tracking-tight text-slate-900 dark:text-white">Welcome Back</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-300">Sign in to access your dashboard</p>
              </div>
              <Chip color="primary" variant="flat" className="font-medium">Hospital</Chip>
            </div>
          </CardHeader>
          <Divider />
          <CardBody className="pt-5">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.15, duration: 0.35 }}
              className="grid gap-3"
            >
              <Input
                type="email"
                label="Email"
                placeholder="Enter your email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                variant="bordered"
                size="lg"
              />
              <Input
                type="password"
                label="Password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                variant="bordered"
                size="lg"
                onKeyDown={(e) => {
                  if (e.key === "Enter") submit();
                }}
              />
              {error && <p className="m-0 text-sm font-medium text-danger">{error}</p>}
              <Button
                color="primary"
                onPress={submit}
                isLoading={isSubmitting}
                fullWidth
                size="lg"
                className="mt-1 font-semibold"
              >
                Login
              </Button>
              <p className="mt-2 text-center text-xs text-slate-500">
                Demo credentials are pre-filled for quick access.
              </p>
            </motion.div>
          </CardBody>
        </Card>
      </motion.div>
    </div>
  );
}

function AdminDashboard({ token }) {
  const [tab, setTab] = useState("departments");
  const [departments, setDepartments] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [receptionists, setReceptionists] = useState([]);
  const [form, setForm] = useState({});
  const [error, setError] = useState("");

  const loadAll = async () => {
    setError("");
    try {
      const [d1, d2, d3] = await Promise.all([
        apiRequest("/api/admin/departments", "GET", null, token),
        apiRequest("/api/admin/doctors", "GET", null, token),
        apiRequest("/api/admin/receptionists", "GET", null, token)
      ]);
      setDepartments(d1);
      setDoctors(d2);
      setReceptionists(d3);
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const addDepartment = async () => {
    await apiRequest("/api/admin/departments", "POST", { name: form.departmentName }, token);
    setForm((f) => ({ ...f, departmentName: "" }));
    await loadAll();
  };

  const addDoctor = async () => {
    await apiRequest(
      "/api/admin/doctors",
      "POST",
      {
        name: form.doctorName,
        email: form.doctorEmail,
        password: form.doctorPassword,
        specialization: form.doctorSpecialization,
        departmentId: Number(form.doctorDepartmentId)
      },
      token
    );
    setForm((f) => ({
      ...f,
      doctorName: "",
      doctorEmail: "",
      doctorPassword: "",
      doctorSpecialization: ""
    }));
    await loadAll();
  };

  const addReceptionist = async () => {
    await apiRequest(
      "/api/admin/receptionists",
      "POST",
      { name: form.receptionistName, email: form.receptionistEmail, password: form.receptionistPassword },
      token
    );
    setForm((f) => ({ ...f, receptionistName: "", receptionistEmail: "", receptionistPassword: "" }));
    await loadAll();
  };

  return (
    <div style={shellStyle} className="text-slate-800 [&_input]:rounded-lg [&_input]:border [&_input]:border-slate-300 [&_input]:px-3 [&_input]:py-2 [&_input]:outline-none [&_input]:transition [&_input]:focus:border-blue-500 [&_input]:focus:ring-2 [&_input]:focus:ring-blue-200 [&_select]:rounded-lg [&_select]:border [&_select]:border-slate-300 [&_select]:px-3 [&_select]:py-2 [&_select]:outline-none">
      <h2 className="mb-3 text-3xl font-bold tracking-tight">Admin Dashboard</h2>
      <div className="mb-4 flex flex-wrap gap-2">
        <Button size="sm" radius="full" variant={tab === "departments" ? "solid" : "flat"} color={tab === "departments" ? "primary" : "default"} onPress={() => setTab("departments")}>Departments</Button>
        <Button size="sm" radius="full" variant={tab === "doctors" ? "solid" : "flat"} color={tab === "doctors" ? "primary" : "default"} onPress={() => setTab("doctors")}>Doctors</Button>
        <Button size="sm" radius="full" variant={tab === "receptionists" ? "solid" : "flat"} color={tab === "receptionists" ? "primary" : "default"} onPress={() => setTab("receptionists")}>Receptionists</Button>
      </div>
      {error && <p className="mb-3 rounded-lg bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600">{error}</p>}

      {tab === "departments" && (
        <div style={cardStyle}>
          <h3 className="mb-3 text-xl font-semibold">Departments</h3>
          <input
            value={form.departmentName || ""}
            onChange={(e) => setForm((f) => ({ ...f, departmentName: e.target.value }))}
            placeholder="Department name"
          />{" "}
          <Button color="primary" onPress={addDepartment}>Add</Button>
          <ul className="mt-4 space-y-2">
            {departments.map((d) => (
              <li key={d.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                {d.name}{" "}
                <Button
                  color="danger"
                  size="sm"
                  onClick={async () => {
                    await apiRequest(`/api/admin/departments/${d.id}`, "DELETE", null, token);
                    await loadAll();
                  }}
                >
                  Delete
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {tab === "doctors" && (
        <div style={cardStyle}>
          <h3 className="mb-3 text-xl font-semibold">Doctors</h3>
          <div className="grid grid-cols-6 gap-2">
            <input placeholder="Name" value={form.doctorName || ""} onChange={(e) => setForm((f) => ({ ...f, doctorName: e.target.value }))} />{" "}
            <input placeholder="Email" value={form.doctorEmail || ""} onChange={(e) => setForm((f) => ({ ...f, doctorEmail: e.target.value }))} />{" "}
            <input
              placeholder="Password"
              type="password"
              value={form.doctorPassword || ""}
              onChange={(e) => setForm((f) => ({ ...f, doctorPassword: e.target.value }))}
            />{" "}
            <input
              placeholder="Specialization"
              value={form.doctorSpecialization || ""}
              onChange={(e) => setForm((f) => ({ ...f, doctorSpecialization: e.target.value }))}
            />{" "}
              <div className='flex items-center gap-2'>
                <Select
              aria-label="Department"
              placeholder="Department"
              className="min-w-[200px]"
              selectedKeys={form.doctorDepartmentId ? [String(form.doctorDepartmentId)] : []}
              onSelectionChange={(keys) => {
                const selected = Array.from(keys)[0];
                setForm((f) => ({ ...f, doctorDepartmentId: selected ? String(selected) : "" }));
              }}
            >
              {departments.map((d) => (
                <SelectItem key={String(d.id)}>{d.name}</SelectItem>
              ))}
            </Select>{" "}
            <Button color="primary" onPress={addDoctor}>Add</Button>
              </div>
          </div>
          <ul className="mt-4 space-y-2">
            {doctors.map((d) => (
              <li key={d.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                {d.name} ({d.specialization || "N/A"}){" "}
                <Button
                  color="danger"
                  size="sm"
                  onClick={async () => {
                    await apiRequest(`/api/admin/doctors/${d.id}`, "DELETE", null, token);
                    await loadAll();
                  }}
                >
                  Delete
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {tab === "receptionists" && (
        <div style={cardStyle}>
          <h3 className="mb-3 text-xl font-semibold">Receptionists</h3>
          <div className="flex flex-wrap gap-2">
            <input
              placeholder="Name"
              value={form.receptionistName || ""}
              onChange={(e) => setForm((f) => ({ ...f, receptionistName: e.target.value }))}
            />{" "}
            <input
              placeholder="Email"
              value={form.receptionistEmail || ""}
              onChange={(e) => setForm((f) => ({ ...f, receptionistEmail: e.target.value }))}
            />{" "}
            <input
              placeholder="Password"
              type="password"
              value={form.receptionistPassword || ""}
              onChange={(e) => setForm((f) => ({ ...f, receptionistPassword: e.target.value }))}
            />{" "}
            <Button color="primary" onPress={addReceptionist}>Add</Button>
          </div>
          <ul className="mt-4 space-y-2">
            {receptionists.map((r) => (
              <li key={r.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                {r.name}{" "}
                <Button
                  color="danger"
                  size="sm"
                  onClick={async () => {
                    await apiRequest(`/api/admin/receptionists/${r.id}`, "DELETE", null, token);
                    await loadAll();
                  }}
                >
                  Delete
                </Button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function ReceptionistDashboard({ token }) {
  const [departments, setDepartments] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [slots, setSlots] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [deptId, setDeptId] = useState("");
  const [doctorId, setDoctorId] = useState("");
  const [patientName, setPatientName] = useState("");
  const [patientPhone, setPatientPhone] = useState("");
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [rescheduleSlot, setRescheduleSlot] = useState({});
  const [error, setError] = useState("");

  const loadDepartments = async () => setDepartments(await apiRequest("/api/receptionist/departments", "GET", null, token));
  const loadAppointments = async () => setAppointments(await apiRequest("/api/receptionist/appointments", "GET", null, token));

  useEffect(() => {
    loadDepartments();
    loadAppointments();
  }, []);

  useEffect(() => {
    if (!deptId) return;
    apiRequest(`/api/receptionist/departments/${deptId}/doctors`, "GET", null, token).then(setDoctors).catch((e) => setError(e.message));
  }, [deptId]);

  useEffect(() => {
    if (!doctorId) return;
    apiRequest(`/api/receptionist/doctors/${doctorId}/slots`, "GET", null, token).then(setSlots).catch((e) => setError(e.message));
  }, [doctorId, appointments]);

  const slotColor = (s) => (s.isPast ? "bg-slate-300 text-slate-700" : s.booked ? "bg-rose-500 text-white" : "bg-emerald-500 text-white");

  const availableSlotTimes = useMemo(() => slots.filter((s) => s.available).map((s) => s.slotStart), [slots]);

  const createBooking = async () => {
    if (!doctorId || !selectedSlot || !patientName || !patientPhone) return;
    setError("");
    try {
      await apiRequest(
        "/api/receptionist/appointments",
        "POST",
        { doctorId: String(doctorId), slotStart: selectedSlot.slotStart, patientName, patientPhone },
        token
      );
      setPatientName("");
      setPatientPhone("");
      setSelectedSlot(null);
      await loadAppointments();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div style={shellStyle} className="text-slate-800 [&_input]:rounded-lg [&_input]:border [&_input]:border-slate-300 [&_input]:px-3 [&_input]:py-2 [&_input]:outline-none [&_input]:transition [&_input]:focus:border-blue-500 [&_input]:focus:ring-2 [&_input]:focus:ring-blue-200 [&_select]:rounded-lg [&_select]:border [&_select]:border-slate-300 [&_select]:px-3 [&_select]:py-2 [&_select]:outline-none">
      <h2 className="mb-3 text-3xl font-bold tracking-tight">Receptionist Dashboard</h2>
      {error && <p className="mb-3 rounded-lg bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600">{error}</p>}

      <div style={cardStyle}>
        <h3 className="mb-3 text-xl font-semibold">Book Appointment</h3>
        <div className="flex flex-wrap items-end gap-3">
          <Select
            label="Department"
            placeholder="Select"
            className="max-w-xs"
            selectedKeys={deptId ? [String(deptId)] : []}
            onSelectionChange={(keys) => {
              const selected = Array.from(keys)[0];
              setDeptId(selected ? String(selected) : "");
              setDoctorId("");
            }}
          >
            {departments.map((d) => (
              <SelectItem key={String(d.id)}>{d.name}</SelectItem>
            ))}
          </Select>
          <Select
            label="Doctor"
            placeholder="Select"
            className="max-w-xs"
            selectedKeys={doctorId ? [String(doctorId)] : []}
            onSelectionChange={(keys) => {
              const selected = Array.from(keys)[0];
              setDoctorId(selected ? String(selected) : "");
            }}
          >
            {doctors.map((d) => (
              <SelectItem key={String(d.id)}>{d.name}</SelectItem>
            ))}
          </Select>
        </div>

        <div className="mt-3 grid gap-2 md:grid-cols-4">
          {slots.map((s) => (
            <Button
              key={s.slotStart}
              size="sm"
              variant="flat"
              disabled={!s.available}
              onPress={() => setSelectedSlot(s)}
              className={`justify-start rounded-lg px-3 py-2 text-sm font-medium transition ${slotColor(s)} ${s.available ? "hover:brightness-95" : "cursor-not-allowed opacity-80"}`}
            >
              {s.slotStart} - {s.slotEnd}
            </Button>
          ))}
        </div>
      </div>

      {selectedSlot && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-5 shadow-2xl">
            <h3 className="mb-3 text-lg font-semibold">Book Slot {selectedSlot.slotStart} - {selectedSlot.slotEnd}</h3>
            <input
              className="mb-2 w-full"
              placeholder="Patient Name"
              value={patientName}
              onChange={(e) => setPatientName(e.target.value)}
            />
            <input
              className="mb-3 w-full"
              placeholder="Patient Phone"
              value={patientPhone}
              onChange={(e) => setPatientPhone(e.target.value)}
            />
            <div className="flex gap-2">
              <Button color="primary" onPress={createBooking}>Confirm Booking</Button>
              <Button variant="flat" onPress={() => setSelectedSlot(null)}>Close</Button>
            </div>
          </div>
        </div>
      )}

      <div style={cardStyle}>
        <h3 className="mb-3 text-xl font-semibold">Today Bookings</h3>
        <ul className="space-y-2">
          {appointments.map((a) => (
            <li key={a.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
              {a.slotStart}-{a.slotEnd} | Dr. {a.doctorName} | {a.patientName} ({a.patientPhone}){" "}
              <Button
                color="danger"
                size="sm"
                className="ml-1"
                onClick={async () => {
                  await apiRequest(`/api/receptionist/appointments/${a.id}`, "DELETE", null, token);
                  await loadAppointments();
                }}
              >
                Cancel
              </Button>{" "}
              <div className='flex items-center gap-2'>
              <Select
                aria-label="New slot"
                placeholder="New slot"
                size="sm"
                className="inline-flex max-w-[100px] align-middle"
                selectedKeys={rescheduleSlot[a.id] ? [String(rescheduleSlot[a.id])] : []}
                onSelectionChange={(keys) => {
                  const selected = Array.from(keys)[0];
                  setRescheduleSlot((s) => ({ ...s, [a.id]: selected ? String(selected) : "" }));
                }}
              >
                {availableSlotTimes.map((s) => (
                  <SelectItem key={s}>{s}</SelectItem>
                ))}
              </Select>{" "}
              <Button
                color="primary"
                size="sm"
                className="ml-1"
                onClick={async () => {
                  const ns = rescheduleSlot[a.id];
                  if (!ns) return;
                  await apiRequest(`/api/receptionist/appointments/${a.id}/reschedule`, "PUT", {
                    newDoctorId: String(doctorId || a.doctorId),
                    newSlotStart: ns
                  }, token);
                  await loadAppointments();
                }}
              >
                Reschedule
              </Button>
              </div>
             
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function DoctorDashboard({ token }) {
  const [appointments, setAppointments] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    apiRequest("/api/doctor/my-appointments", "GET", null, token).then(setAppointments).catch((e) => setError(e.message));
  }, []);

  return (
    <div style={shellStyle}>
      <h2 className="mb-3 text-3xl font-bold tracking-tight text-slate-800">Doctor Dashboard</h2>
      {error && <p className="mb-3 rounded-lg bg-rose-50 px-3 py-2 text-sm font-medium text-rose-600">{error}</p>}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg">
      <table className="w-full border-collapse text-left">
        <thead>
          <tr className="bg-slate-900 text-white">
            <th className="px-4 py-3">Time</th>
            <th className="px-4 py-3">Patient</th>
          </tr>
        </thead>
        <tbody>
          {appointments.map((a) => (
            <tr key={a.id} className="border-t border-slate-200 odd:bg-slate-50">
              <td className="px-4 py-3">{a.slotStart} - {a.slotEnd}</td>
              <td className="px-4 py-3">{a.patientName || "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
}

function App() {
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [role, setRole] = useState(localStorage.getItem("role") || "");

  const onLogin = (newToken, newRole) => {
    localStorage.setItem("token", newToken);
    localStorage.setItem("role", newRole);
    setToken(newToken);
    setRole(newRole);
  };

  const logout = () => {
    localStorage.clear();
    setToken("");
    setRole("");
  };

  if (!token) return <LoginView onLogin={onLogin} />;

  return (
    <div className="min-h-screen bg-slate-100">
      <div className="border-b border-slate-200 bg-white/95 px-5 py-3 shadow-sm backdrop-blur">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-end gap-3">
          <span className="rounded-full bg-slate-900 px-3 py-1 text-xs font-semibold text-white">{role}</span>
          <Button color="primary" size="sm" onPress={logout}>Logout</Button>
        </div>
      </div>
      {role === "ADMIN" && <AdminDashboard token={token} />}
      {role === "RECEPTIONIST" && <ReceptionistDashboard token={token} />}
      {role === "DOCTOR" && <DoctorDashboard token={token} />}
    </div>
  );
}

export default App;
