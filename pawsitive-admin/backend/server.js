require('dotenv').config();
const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer');

const app = express();
app.use(cors());
app.use(express.json());

// Initialize Firebase Admin
try {
    const serviceAccount = require('./serviceAccountKey.json');
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: `https://${serviceAccount.project_id}.firebaseio.com`
    });
    console.log('Firebase Admin SDK initialized successfully');
} catch (error) {
    console.error('Firebase initialization failed:', error.message);
}

const db = admin.firestore();
const auth = admin.auth();
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-this';

// ============= EMAIL CONFIGURATION =============
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.GMAIL_USER || 'shraddhabhagwat.215081@gmail.com',
        pass: process.env.GMAIL_PASSWORD || 'tccxsxykofqpivaf'
    }
});

async function sendVerificationEmail(email, uid, name) {
    try {
        const verificationToken = await auth.createCustomToken(uid);
        const verificationLink = `http://10.143.57.191:3000/api/auth/verify-email?uid=${uid}&token=${verificationToken}`;
        const emailContent = `
        <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; background-color: #1a1c23; border-radius: 20px; overflow: hidden; color: #ffffff;">
            <div style="background-color: #FFC727; padding: 40px 20px; text-align: center;">
                <h1 style="color: #ffffff; margin: 0; font-size: 42px; font-weight: 900;">Pawsitive 🐾</h1>
            </div>
            <div style="padding: 40px 30px;">
                <h2 style="color: #FFC727; font-size: 26px; margin-bottom: 20px;">Verify Your Email</h2>
                <p>Hello <strong>${name || 'User'}</strong>,</p>
                <p>Welcome! Please verify your email to secure your account:</p>
                <div style="text-align: center; margin: 40px 0;">
                    <a href="${verificationLink}" style="background-color: #FFC727; color: #1a1c23; padding: 15px 45px; text-decoration: none; border-radius: 30px; font-weight: bold; font-size: 18px; display: inline-block;">✓ Verify My Account</a>
                </div>
            </div>
        </div>`;
        await transporter.sendMail({ from: '"Pawsitive App" <shraddhabhagwat.215081@gmail.com>', to: email, subject: '🐾 Verify Your Account', html: emailContent });
        return true;
    } catch (e) { return false; }
}

// ============= API ROUTES =============

app.get('/api/auth/verify-email', async (req, res) => {
    try {
        const { uid } = req.query;
        await auth.updateUser(uid, { emailVerified: true });
        const userRef = db.collection('users').doc(uid);
        const ngoRef = db.collection('ngo_profiles').doc(uid);
        const [uDoc, nDoc] = await Promise.all([userRef.get(), ngoRef.get()]);
        if (uDoc.exists) await userRef.update({ email_verified: true });
        if (nDoc.exists) await ngoRef.update({ email_verified: true });
        res.send('<h1>Verified!</h1>');
    } catch (e) { res.status(500).send('Failed'); }
});

app.post('/api/auth/check-email', async (req, res) => {
    try {
        const { email } = req.body;
        const userRefs = await db.collection('users').where('email', '==', email).get();
        const ngoRefs = await db.collection('ngo_profiles').where('ngo_email', '==', email).get();
        res.json({ exists: !userRefs.empty || !ngoRefs.empty });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/auth/login', async (req, res) => {
    try {
        const { email } = req.body;
        let authUser;
        try {
            authUser = await auth.getUserByEmail(email);
        } catch (e) {
            return res.status(401).json({ error: 'User not found' });
        }

        const [userSnap, ngoSnap, adminSnap] = await Promise.all([
            db.collection('users').doc(authUser.uid).get(),
            db.collection('ngo_profiles').doc(authUser.uid).get(),
            db.collection('admin_accounts').where('email', '==', email).get()
        ]);

        let role = 'USER';
        if (!adminSnap.empty) role = 'ADMIN';
        else if (ngoSnap.exists) role = 'NGO';
        else if (userSnap.exists) role = userSnap.data().role || 'USER';

        const token = jwt.sign({ uid: authUser.uid, email, role }, JWT_SECRET, { expiresIn: '7d' });
        res.json({ success: true, token, role, uid: authUser.uid });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.post('/api/user/register', async (req, res) => {
    try {
        const { email, password, full_name, phone, description, profile_photo_url, latitude, longitude, location_address } = req.body;
        const userRecord = await auth.createUser({ email, password, displayName: full_name });
        await db.collection('users').doc(userRecord.uid).set({
            uid: userRecord.uid, full_name, email, phone, description, profile_photo_url,
            latitude, longitude, location_address, role: 'USER', created_at: admin.firestore.FieldValue.serverTimestamp()
        });
        await sendVerificationEmail(email, userRecord.uid, full_name);
        res.json({ success: true });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/ngo/register', async (req, res) => {
    try {
        const { email, password, organization_name, phone, address, license_number, description, profile_photo_url, certificate_url } = req.body;
        const userRecord = await auth.createUser({ email, password });
        await db.collection('ngo_profiles').doc(userRecord.uid).set({
            uid: userRecord.uid, organization_name, phone, address, license_number, description,
            ngo_email: email, profile_photo_url, certificate_url, verification_status: 'PENDING',
            role: 'NGO', created_at: admin.firestore.FieldValue.serverTimestamp()
        });
        await sendVerificationEmail(email, userRecord.uid, organization_name);
        res.json({ success: true });
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.get('/api/ngo/profile', async (req, res) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];
        if (!token) return res.status(401).json({ error: 'Unauthorized' });
        const decoded = jwt.verify(token, JWT_SECRET);
        const ngoDoc = await db.collection('ngo_profiles').doc(decoded.uid).get();
        if (!ngoDoc.exists) return res.status(404).json({ error: 'NGO profile not found' });
        res.json({ success: true, data: ngoDoc.data() });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/ngos', async (req, res) => {
    try {
        const ngos = await db.collection('ngo_profiles').get();
        const list = [];
        ngos.forEach(doc => list.push({ id: doc.id, ...doc.data() }));
        res.json({ success: true, data: list });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/admin/stats', async (req, res) => {
    try {
        const ngos = await db.collection('ngo_profiles').get();
        let verified = 0, pending = 0, rejected = 0;
        ngos.forEach(doc => {
            const status = doc.data().verification_status;
            if (status === 'VERIFIED') verified++;
            else if (status === 'PENDING') pending++;
            else if (status === 'REJECTED') rejected++;
        });
        res.json({ success: true, data: { totalNGOs: ngos.size, verifiedNGOs: verified, pendingNGOs: pending, rejectedNGOs: rejected, totalAnimalsPosts: 0, animalsSaved: 0 } });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.post('/api/ngos/:id/approve', async (req, res) => {
    try {
        await db.collection('ngo_profiles').doc(req.params.id).update({ verification_status: 'VERIFIED' });
        res.json({ success: true, message: 'NGO verified' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.post('/api/ngos/:id/reject', async (req, res) => {
    try {
        await db.collection('ngo_profiles').doc(req.params.id).update({ verification_status: 'REJECTED', rejection_reason: req.body.rejection_reason });
        res.json({ success: true, message: 'NGO rejected' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Middleware to verify JWT token
const verifyToken = (req, res, next) => {
    const token = req.headers.authorization?.split(' ')[1];
    if (!token) return res.status(401).json({ error: 'No token provided' });
    try {
        req.user = jwt.verify(token, JWT_SECRET);
        next();
    } catch (error) {
        res.status(401).json({ error: 'Invalid token' });
    }
};

// Add NGO Staff
app.post('/api/ngo/staff', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'NGO') return res.status(403).json({ error: 'Only NGOs can add staff' });
        const { email, password, full_name, phone, staff_role, status } = req.body;
        if (!email || !password || !full_name || !staff_role) return res.status(400).json({ error: 'Missing required fields' });

        const ngoDoc = await db.collection('ngo_profiles').doc(req.user.uid).get();
        if (!ngoDoc.exists) return res.status(404).json({ error: 'NGO profile not found' });

        const userRecord = await auth.createUser({ email, password, displayName: full_name, phoneNumber: phone || undefined });
        const createdAt = admin.firestore.FieldValue.serverTimestamp();

        await db.collection('users').doc(userRecord.uid).set({
            uid: userRecord.uid, full_name, email, phone: phone || null, profile_photo_url: null,
            role: 'STAFF', ngo_id: req.user.uid, ngo_name: ngoDoc.data().organization_name, created_at: createdAt
        });
        await db.collection('ngo_staff').doc(userRecord.uid).set({
            uid: userRecord.uid, full_name, email, phone: phone || null, role: staff_role,
            status: status || 'active', ngo_id: req.user.uid, ngo_name: ngoDoc.data().organization_name, created_at: createdAt
        });

        res.json({ success: true, message: 'Staff added successfully', uid: userRecord.uid });
    } catch(error) {
        res.status(500).json({ error: error.message });
    }
});

// List NGO Staff
app.get('/api/ngo/staff', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'NGO') return res.status(403).json({ error: 'Only NGOs can view their staff' });
        const staffsSnapshot = await db.collection('ngo_staff').where('ngo_id', '==', req.user.uid).get();
        const staffList = [];
        staffsSnapshot.forEach(doc => staffList.push({ id: doc.id, ...doc.data() }));
        res.json({ success: true, data: staffList });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Update NGO Staff
app.put('/api/ngo/staff/:id', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'NGO') return res.status(403).json({ error: 'Only NGOs can manage staff' });
        const staffRef = db.collection('ngo_staff').doc(req.params.id);
        const staffDoc = await staffRef.get();
        if (!staffDoc.exists || staffDoc.data().ngo_id !== req.user.uid) return res.status(404).json({ error: 'Staff not found' });

        const updates = {}, userUpdates = {};
        if (req.body.staff_role || req.body.role) updates.role = req.body.staff_role || req.body.role;
        if (req.body.status) updates.status = req.body.status;
        if (req.body.phone) { updates.phone = req.body.phone; userUpdates.phone = req.body.phone; }

        if (Object.keys(updates).length > 0) await staffRef.update(updates);
        if (Object.keys(userUpdates).length > 0) await db.collection('users').doc(req.params.id).update(userUpdates);

        res.json({ success: true, message: 'Staff updated successfully' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Delete NGO Staff
app.delete('/api/ngo/staff/:id', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'NGO') return res.status(403).json({ error: 'Only NGOs can remove staff' });
        const staffRef = db.collection('ngo_staff').doc(req.params.id);
        const staffDoc = await staffRef.get();
        if (!staffDoc.exists || staffDoc.data().ngo_id !== req.user.uid) return res.status(404).json({ error: 'Staff not found' });

        await staffRef.delete();
        await db.collection('users').doc(req.params.id).delete();
        try { await auth.deleteUser(req.params.id); } catch(e) {}
        res.json({ success: true, message: 'Staff removed successfully' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.use((req, res) => {
    res.status(404).json({ error: `Route ${req.originalUrl} not found` });
});

const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`Server on ${PORT}`));
