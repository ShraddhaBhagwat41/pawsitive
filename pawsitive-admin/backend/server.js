require('dotenv').config();
const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer');

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Initialize Firebase Admin
// Try to initialize with credentials from environment or use default
try {
    // First, try to read credentials from GOOGLE_APPLICATION_CREDENTIALS env var
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
        admin.initializeApp();
    } else {
        // Fallback: Initialize with just the project ID (works if running in Google Cloud or with ambient credentials)
        const serviceAccount = require('./serviceAccountKey.json');
        const projectId = process.env.FIREBASE_PROJECT_ID || serviceAccount.project_id;
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            databaseURL: `https://${projectId}.firebaseio.com`
        });
    }
    console.log('Firebase Admin SDK initialized successfully');
} catch (error) {
    console.warn('Firebase initialization warning:', error.message);
    console.log('Attempting to initialize without service account key...');
    try {
        // Try direct initialization - may work with ambient credentials
        admin.initializeApp({
            projectId: process.env.FIREBASE_PROJECT_ID
        });
        console.log('Firebase initialized with project ID');
    } catch (err) {
        console.error('Firebase Admin SDK initialization failed:', err.message);
        console.error('Please ensure you have a valid serviceAccountKey.json in the backend directory');
        console.error('Or set GOOGLE_APPLICATION_CREDENTIALS environment variable');
    }
}

const db = admin.firestore();
const auth = admin.auth();

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-this';

console.log("Using Password: ", process.env.GMAIL_PASSWORD); // Add this temporarily!
// ============= EMAIL CONFIGURATION =============
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.GMAIL_USER || 'shraddhabhagwat.215081@gmail.com',
        pass: process.env.GMAIL_PASSWORD || 'tccxsxykofqpivaf'
    }
});

// Email sending helper function
async function sendEmail(to, subject, htmlContent) {
    try {
        const mailOptions = {
            from: `${process.env.EMAIL_FROM_NAME || 'Pawsitive'} <${process.env.GMAIL_USER}>`,
            to: to,
            subject: subject,
            html: htmlContent
        };
        
        await transporter.sendMail(mailOptions);
        console.log(`Email sent to ${to}: ${subject}`);
        return true;
    } catch (error) {
        console.error(`Failed to send email to ${to}:`, error.message);
        return false;
    }
}

// Generate verification token and send verification email
async function sendVerificationEmail(email, uid) {
    try {
        // Generate a verification token using Firebase
        const verificationToken = await auth.createCustomToken(uid);
        
        // Create verification link - backend confirms email verification
        const verificationLink = `${process.env.BACKEND_URL || 'http://localhost:3000'}/api/auth/verify-email?token=${verificationToken}&email=${encodeURIComponent(email)}&uid=${uid}`;
        
        const emailContent = `
        <h2>📧 Verify Your Email - Pawsitive</h2>
        <p>Hello,</p>
        <p>Thank you for registering with Pawsitive! Your account is almost ready.</p>
        <p><strong>Click the button below to verify your email address:</strong></p>
        <p style="margin: 30px 0;">
            <a href="${verificationLink}" style="background-color: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">✓ Verify Email</a>
        </p>
        <p>Or copy and paste this link in your browser:</p>
        <p style="word-break: break-all; background-color: #f0f0f0; padding: 10px; border-radius: 3px;">
            ${verificationLink}
        </p>
        <p><strong>Important:</strong> After clicking the link, go back to the Pawsitive app to continue.</p>
        <p>If you didn't create this account, please ignore this email.</p>
        <p>This link expires in 24 hours.</p>
        <p>Best regards,<br><strong>Pawsitive Team 🐾</strong></p>
        `;
        
        const sent = await sendEmail(email, '✓ Verify Your Pawsitive Account', emailContent);
        
        if (sent) {
            console.log(`✓ Verification email sent to ${email}`);
            return true;
        } else {
            console.error(`❌ Failed to send verification email to ${email}`);
            return false;
        }
    } catch (error) {
        console.error('Error sending verification email:', error.message);
        return false;
    }
}

// ============= AUTH ROUTES =============

// Login endpoint
app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password required' });
        }

        // Verify with Firebase Auth
        // Note: This requires using Firebase REST API or custom auth logic
        // For now, we'll verify user exists in database
        const userRefs = await db.collection('admin_accounts').where('email', '==', email).get();
        const ngoRefs = await db.collection('ngo_profiles').where('ngo_email', '==', email).get();
        const userProfileRefs = await db.collection('users').where('email', '==', email).get();

        let userData = null;
        let role = null;

        if (!userRefs.empty) {
            userData = userRefs.docs[0];
            role = 'ADMIN';
        } else if (!ngoRefs.empty) {
            userData = ngoRefs.docs[0];
            role = 'NGO';
        } else if (!userProfileRefs.empty) {
            userData = userProfileRefs.docs[0];
            role = 'USER';
        }

        if (!userData) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        // Generate JWT token
        const token = jwt.sign(
            { uid: userData.id, email, role },
            JWT_SECRET,
            { expiresIn: '7d' }
        );

        res.json({
            success: true,
            token,
            role,
            uid: userData.id,
            email
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Middleware to verify JWT token
const verifyToken = (req, res, next) => {
    const token = req.headers.authorization?.split(' ')[1];

    if (!token) {
        return res.status(401).json({ error: 'No token provided' });
    }

    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded;
        next();
    } catch (error) {
        res.status(401).json({ error: 'Invalid token' });
    }
};

// ============= EMAIL VERIFICATION ROUTES =============

// Verify email endpoint - called when user clicks link in email
app.get('/api/auth/verify-email', async (req, res) => {
    try {
        const { email, uid, token } = req.query;

        if (!email || !uid) {
            return res.status(400).send(`
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Verification Error - Pawsitive</title>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { 
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            display: flex; justify-content: center; align-items: center; 
                            height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        }
                        .container { 
                            text-align: center; background: white; padding: 40px; 
                            border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.2);
                            max-width: 400px; width: 90%;
                        }
                        h1 { color: #f44336; margin: 0; font-size: 28px; }
                        p { color: #666; font-size: 15px; margin: 15px 0; line-height: 1.6; }
                        .icon { font-size: 50px; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">❌</div>
                        <h1>Invalid Link</h1>
                        <p>The verification link is missing required information.</p>
                        <p>Please try signing up again or contact support.</p>
                    </div>
                </body>
                </html>
            `);
        }

        console.log('Email verification request:', { email, uid });

        let userType = 'USER'; // default
        let verified = false;

        // First, try to check if it's a regular user
        const userRef = db.collection('users').doc(uid);
        const userDoc = await userRef.get();

        if (userDoc.exists) {
            // Update verification status for regular user
            await userRef.update({
                email_verified: true,
                email_verified_at: admin.firestore.FieldValue.serverTimestamp()
            });
            verified = true;
            userType = 'USER';
            console.log(`✓ Email verified for USER ${uid}`);
        } else {
            // Check if it's an NGO
            const ngoRef = db.collection('ngo_profiles').doc(uid);
            const ngoDoc = await ngoRef.get();

            if (ngoDoc.exists) {
                // Update verification status for NGO
                await ngoRef.update({
                    email_verified: true,
                    email_verified_at: admin.firestore.FieldValue.serverTimestamp()
                });
                verified = true;
                userType = 'NGO';
                console.log(`✓ Email verified for NGO ${uid}`);
            }
        }

        if (!verified) {
            return res.status(404).send(`
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Verification Error - Pawsitive</title>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { 
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                            display: flex; justify-content: center; align-items: center; 
                            height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        }
                        .container { 
                            text-align: center; background: white; padding: 40px; 
                            border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.2);
                            max-width: 400px; width: 90%;
                        }
                        h1 { color: #f44336; margin: 0; font-size: 28px; }
                        p { color: #666; font-size: 15px; margin: 15px 0; line-height: 1.6; }
                        .icon { font-size: 50px; margin: 20px 0; }
                        .button {
                            background: #667eea;
                            color: white;
                            padding: 12px 25px;
                            text-decoration: none;
                            border-radius: 8px;
                            display: inline-block;
                            margin-top: 20px;
                            font-weight: 600;
                            cursor: pointer;
                        }
                        .button:hover { background: #764ba2; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">❌</div>
                        <h1>Verification Failed</h1>
                        <p>User account not found.</p>
                        <p>Please make sure you're using the correct email link or try signing up again.</p>
                        <a href="intent://com.pawsitive.app/#Intent;scheme=app;package=com.pawsitive.app;end" class="button">
                            ← Back to Pawsitive App
                        </a>
                    </div>
                </body>
                </html>
            `);
        }

        // Also try to mark in Firebase Auth if possible
        try {
            await auth.updateUser(uid, { emailVerified: true });
            console.log(`✓ Email verified in Firebase Auth for ${uid}`);
        } catch (authError) {
            console.warn('Could not update Firebase Auth:', authError.message);
        }

        // Return success page
        res.send(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Email Verified - Pawsitive</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        display: flex; justify-content: center; align-items: center; 
                        min-height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        padding: 20px;
                    }
                    .container { 
                        text-align: center; background: white; padding: 50px 30px; 
                        border-radius: 20px; box-shadow: 0 15px 35px rgba(0,0,0,0.2);
                        max-width: 480px; width: 100%;
                    }
                    .checkmark { 
                        font-size: 80px; color: #4CAF50; margin: 20px 0; 
                        animation: bounce 0.6s ease-in-out;
                        display: inline-block;
                    }
                    @keyframes bounce { 
                        0%, 100% { transform: scale(1); } 
                        50% { transform: scale(1.15); } 
                    }
                    h1 { 
                        color: #4CAF50; margin: 20px 0 10px; font-size: 32px;
                        font-weight: 700;
                    }
                    .subtitle { 
                        color: #666; font-size: 16px; margin: 15px 0;
                        font-weight: 500;
                    }
                    .email-box { 
                        font-weight: bold; color: #667eea; 
                        background: linear-gradient(135deg, #f5f7ff 0%, #e8ecff 100%);
                        padding: 12px 16px; border-radius: 8px; 
                        display: inline-block; margin: 15px 0;
                        border: 2px solid #e0e8ff;
                    }
                    .status-box {
                        background: #f0f8ff;
                        border-left: 5px solid #4CAF50;
                        padding: 15px;
                        margin: 25px 0;
                        border-radius: 8px;
                        text-align: left;
                    }
                    .status-box h3 {
                        margin: 0 0 10px 0;
                        color: #4CAF50;
                        font-size: 16px;
                    }
                    .status-box p {
                        margin: 8px 0;
                        color: #555;
                        font-size: 14px;
                        line-height: 1.6;
                    }
                    .status-box strong {
                        color: #333;
                    }
                    .instructions {
                        background: #fff9e6;
                        border-left: 5px solid #ff9800;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 8px;
                        text-align: left;
                    }
                    .instructions h3 {
                        margin: 0 0 10px 0;
                        color: #ff9800;
                        font-size: 16px;
                    }
                    .instructions ol {
                        margin: 0;
                        padding-left: 20px;
                        font-size: 14px;
                        line-height: 1.8;
                        color: #555;
                    }
                    .instructions li {
                        margin: 8px 0;
                    }
                    .footer {
                        color: #999; font-size: 12px; margin-top: 30px;
                        border-top: 1px solid #eee;
                        padding-top: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="checkmark">✓</div>
                    <h1>Email Verified!</h1>
                    <p class="subtitle">Your email has been successfully verified.</p>
                    
                    <div class="email-box">${email}</div>
                    
                    <div class="status-box">
                        <h3>✓ What's Next?</h3>
                        <p><strong>Account Type:</strong> ${userType === 'NGO' ? '🏢 NGO Organization' : '👤 Individual User'}</p>
                        <p><strong>Status:</strong> Email Verified ✓</p>
                        ${userType === 'NGO' ? '<p><strong>Note:</strong> NGO accounts require admin approval before full access.</p>' : ''}
                    </div>
                    
                    <div class="instructions">
                        <h3>📱 Instructions for Emulator Users</h3>
                        <ol>
                            <li><strong>You clicked this link on your phone</strong> ✓</li>
                            <li><strong>Your email is now verified in our system</strong> ✓</li>
                            <li><strong>Switch to your Android Emulator</strong></li>
                            <li><strong>The app will automatically detect the verification</strong></li>
                            <li>You'll be redirected to Sign In or Dashboard</li>
                        </ol>
                    </div>
                    
                    <p style="margin: 20px 0; font-size: 15px; color: #333;">
                        <strong>Everything is set! 🎉</strong><br>
                        Your ${userType === 'NGO' ? 'NGO' : 'account'} is ready to use.
                    </p>
                    
                    <div class="footer">
                        <p>Pawsitive • Helping Animals in Need 🐾</p>
                        <p style="margin-top: 5px;">You can now close this page.</p>
                    </div>
                </div>
            </body>
            </html>
        `);
    } catch (error) {
        console.error('Email verification error:', error);
        res.status(500).send(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Verification Error - Pawsitive</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        display: flex; justify-content: center; align-items: center; 
                        height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    }
                    .container { 
                        text-align: center; background: white; padding: 40px; 
                        border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.2);
                        max-width: 400px; width: 90%;
                    }
                    h1 { color: #f44336; margin: 0; font-size: 28px; }
                    p { color: #666; font-size: 15px; margin: 15px 0; line-height: 1.6; }
                    .icon { font-size: 50px; margin: 20px 0; }
                    .error-code { 
                        background: #fff3cd; 
                        color: #856404; 
                        padding: 10px; 
                        border-radius: 5px; 
                        font-size: 12px;
                        word-break: break-all;
                        margin: 15px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">⚠️</div>
                    <h1>Verification Error</h1>
                    <p>There was an error verifying your email.</p>
                    <div class="error-code">${error.message}</div>
                    <p>Please try verifying again or contact our support team.</p>
                </div>
            </body>
            </html>
        `);
    }
});

// ============= DEBUG & CLEANUP ROUTES (FOR TESTING) =============

// Debug: Check if email exists in Firestore and Firebase Auth
app.post('/api/debug/check-email', async (req, res) => {
    try {
        const { email } = req.body;
        
        if (!email) {
            return res.status(400).json({ error: 'Email required' });
        }

        console.log('Checking email:', email);
        
        // Check in Firestore users collection
        const userRef = await db.collection('users').where('email', '==', email).get();
        
        // Check in Firestore NGO collection
        const ngoRef = await db.collection('ngo_profiles').where('ngo_email', '==', email).get();
        
        // Check in Firebase Authentication
        let authUser = null;
        try {
            authUser = await auth.getUserByEmail(email);
        } catch (err) {
            console.log('Not found in Firebase Auth (expected):', err.message);
        }
        
        res.json({
            email,
            inFirestoreUsers: !userRef.empty,
            inFirestoreNGO: !ngoRef.empty,
            inFirebaseAuth: authUser ? true : false,
            firebaseAuthUid: authUser?.uid || null,
            firestoreUserId: !userRef.empty ? userRef.docs[0].id : null,
            firestoreNGOId: !ngoRef.empty ? ngoRef.docs[0].id : null
        });
    } catch (error) {
        console.error('Debug check error:', error);
        res.status(500).json({ error: error.message });
    }
});

// Debug: Check if email is verified in Firebase Auth
app.post('/api/debug/check-email-verified', async (req, res) => {
    try {
        const { email } = req.body;
        
        if (!email) {
            return res.status(400).json({ error: 'Email required' });
        }

        console.log('Checking email verification status in Firebase Auth:', email);
        
        try {
            const authUser = await auth.getUserByEmail(email);
            console.log('Firebase Auth user found:', {
                uid: authUser.uid,
                email: authUser.email,
                emailVerified: authUser.emailVerified,
                createdAt: authUser.metadata.creationTime,
                lastSignInTime: authUser.metadata.lastSignInTime
            });
            
            res.json({
                email,
                uid: authUser.uid,
                emailVerified: authUser.emailVerified,
                createdAt: authUser.metadata.creationTime,
                lastSignInTime: authUser.metadata.lastSignInTime,
                found: true
            });
        } catch (err) {
            console.error('Email not found in Firebase Auth:', err.message);
            res.status(404).json({ 
                error: 'Email not found in Firebase Auth',
                email,
                details: err.message
            });
        }
    } catch (error) {
        console.error('Debug email verification check error:', error);
        res.status(500).json({ error: error.message });
    }
});

// Debug: Delete email from both Firestore and Firebase Auth (FOR TESTING ONLY)
app.post('/api/debug/cleanup-email', async (req, res) => {
    try {
        const { email } = req.body;
        
        if (!email) {
            return res.status(400).json({ error: 'Email required' });
        }

        console.log('Cleaning up email:', email);
        
        const cleaned = {
            email,
            deletedFromFirestoreUsers: false,
            deletedFromFirestoreNGO: false,
            deletedFromFirebaseAuth: false,
            errors: []
        };
        
        // Delete from Firestore users collection
        try {
            const userRef = await db.collection('users').where('email', '==', email).get();
            if (!userRef.empty) {
                for (const doc of userRef.docs) {
                    await db.collection('users').doc(doc.id).delete();
                    cleaned.deletedFromFirestoreUsers = true;
                    console.log('Deleted from Firestore users:', doc.id);
                }
            }
        } catch (err) {
            cleaned.errors.push('Users cleanup: ' + err.message);
        }
        
        // Delete from Firestore NGO collection
        try {
            const ngoRef = await db.collection('ngo_profiles').where('ngo_email', '==', email).get();
            if (!ngoRef.empty) {
                for (const doc of ngoRef.docs) {
                    await db.collection('ngo_profiles').doc(doc.id).delete();
                    cleaned.deletedFromFirestoreNGO = true;
                    console.log('Deleted from Firestore NGO:', doc.id);
                }
            }
        } catch (err) {
            cleaned.errors.push('NGO cleanup: ' + err.message);
        }
        
        // Delete from Firebase Authentication
        try {
            const authUser = await auth.getUserByEmail(email);
            await auth.deleteUser(authUser.uid);
            cleaned.deletedFromFirebaseAuth = true;
            console.log('Deleted from Firebase Auth:', authUser.uid);
        } catch (err) {
            if (err.code !== 'auth/user-not-found') {
                cleaned.errors.push('Firebase Auth cleanup: ' + err.message);
            }
        }
        
        res.json(cleaned);
    } catch (error) {
        console.error('Debug cleanup error:', error);
        res.status(500).json({ error: error.message });
    }
});

// ============= NGO ROUTES =============

// Get all NGOs (admin only)
app.get('/api/ngos', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'ADMIN') {
            return res.status(403).json({ error: 'Admin access required' });
        }

        const ngos = await db.collection('ngo_profiles').get();
        const ngoList = [];

        ngos.forEach(doc => {
            ngoList.push({
                id: doc.id,
                ...doc.data()
            });
        });

        res.json({ success: true, data: ngoList });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Get single NGO
app.get('/api/ngos/:id', verifyToken, async (req, res) => {
    try {
        const { id } = req.params;

        const ngo = await db.collection('ngo_profiles').doc(id).get();

        if (!ngo.exists) {
            return res.status(404).json({ error: 'NGO not found' });
        }

        res.json({
            success: true,
            data: { id: ngo.id, ...ngo.data() }
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Register NGO
app.post('/api/ngo/register', async (req, res) => {
    try {
        const { email, password, organization_name, phone, address, license_number, description, profile_photo_url, certificate_url } = req.body;

        console.log('NGO Registration Request:', {
            email,
            password: password ? '***' : 'missing',
            organization_name,
            phone: phone || 'null/undefined',
            address: address || 'null/undefined',
            license_number: license_number || 'null/undefined'
        });

        if (!email || !password || !organization_name) {
            const missingFields = [];
            if (!email) missingFields.push('email');
            if (!password) missingFields.push('password');
            if (!organization_name) missingFields.push('organization_name');
            
            console.warn('Missing required fields:', missingFields);
            return res.status(400).json({ 
                error: 'Missing required fields: ' + missingFields.join(', '),
                receivedFields: Object.keys(req.body)
            });
        }

        // Create Firebase user
        let userRecord;
        try {
            console.log('Attempting to create Firebase user (NGO) with email:', email);
            userRecord = await auth.createUser({
                email,
                password
            });
            console.log('✓ Firebase NGO user created successfully:', userRecord.uid);
        } catch (authError) {
            console.error('❌ Firebase Auth Error (NGO) Details:');
            console.error('   Error Code:', authError.code);
            console.error('   Error Message:', authError.message);
            console.error('   Full Error:', JSON.stringify(authError, null, 2));
            
            if (authError.code === 'auth/email-already-exists' || authError.message.includes('email-already-exists')) {
                return res.status(400).json({ error: 'Email already registered in Firebase' });
            }
            if (authError.code === 'auth/invalid-email') {
                return res.status(400).json({ error: 'Invalid email format' });
            }
            if (authError.code === 'auth/weak-password') {
                return res.status(400).json({ error: 'Password is too weak' });
            }
            
            throw authError;
        }

        // Create NGO profile document (only include defined fields)
        const ngoData = {
            uid: userRecord.uid,
            organization_name,
            phone: phone || null,
            address: address || null,
            license_number: license_number || null,
            description: description || null,
            ngo_email: email,
            profile_photo_url: profile_photo_url || null,
            certificate_url: certificate_url || null,
            verification_status: 'PENDING',
            role: 'NGO',
            created_at: admin.firestore.FieldValue.serverTimestamp()
        };

        await db.collection('ngo_profiles').doc(userRecord.uid).set(ngoData);
        console.log('NGO profile created in Firestore:', userRecord.uid);

        console.log('NGO registered successfully:', email);
        res.json({
            success: true,
            message: '✓ Thank you for registering! Admin will verify your details soon and send you login credentials via email.',
            uid: userRecord.uid,
            verification_status: 'PENDING'
        });
    } catch (error) {
        console.error('NGO Registration Error:', error);
        res.status(500).json({ 
            error: 'Registration failed: ' + error.message,
            details: process.env.NODE_ENV === 'development' ? error.stack : undefined
        });
    }
});

// Register User
app.post('/api/user/register', async (req, res) => {
    try {
        const { email, password, full_name, phone, description, profile_photo_url } = req.body;

        console.log('User Registration Request:', {
            email,
            password: password ? '***' : 'missing',
            full_name,
            phone: phone || 'null/undefined',
            description: description || 'null/undefined',
            profile_photo_url: profile_photo_url || 'null/undefined'
        });

        if (!email || !password || !full_name) {
            const missingFields = [];
            if (!email) missingFields.push('email');
            if (!password) missingFields.push('password');
            if (!full_name) missingFields.push('full_name');
            
            console.warn('Missing required fields:', missingFields);
            return res.status(400).json({ 
                error: 'Missing required fields: ' + missingFields.join(', '),
                receivedFields: Object.keys(req.body)
            });
        }

        if (password.length < 6) {
            return res.status(400).json({ error: 'Password must be at least 6 characters' });
        }

        // Check if user already exists
        const existingUser = await db.collection('users').where('email', '==', email).get();
        if (!existingUser.empty) {
            return res.status(400).json({ error: 'Email already registered' });
        }

        // Create Firebase user
        let userRecord;
        try {
            console.log('Attempting to create Firebase user with email:', email);
            userRecord = await auth.createUser({
                email,
                password,
                displayName: full_name
            });
            console.log('✓ Firebase user created successfully:', userRecord.uid);
        } catch (authError) {
            console.error('❌ Firebase Auth Error Details:');
            console.error('   Error Code:', authError.code);
            console.error('   Error Message:', authError.message);
            console.error('   Full Error:', JSON.stringify(authError, null, 2));
            
            // Log the auth object state
            try {
                const userByEmail = await auth.getUserByEmail(email);
                console.warn('⚠️  User already exists with this email:', userByEmail.uid);
                return res.status(400).json({ error: 'Email already registered in Firebase' });
            } catch (checkError) {
                console.error('User check error:', checkError.message);
            }
            
            if (authError.code === 'auth/email-already-exists' || authError.message.includes('email-already-exists')) {
                return res.status(400).json({ error: 'Email already in use' });
            }
            if (authError.code === 'auth/invalid-email') {
                return res.status(400).json({ error: 'Invalid email format' });
            }
            if (authError.code === 'auth/weak-password') {
                return res.status(400).json({ error: 'Password is too weak. Use at least 6 characters with mixed case.' });
            }
            
            throw authError;
        }

        // Create user profile document (only include defined fields)
        const userData = {
            uid: userRecord.uid,
            full_name,
            email,
            phone: phone || null,
            description: description || null,
            profile_photo_url: profile_photo_url || null,
            role: 'USER',
            email_verified: false,
            created_at: admin.firestore.FieldValue.serverTimestamp()
        };

        await db.collection('users').doc(userRecord.uid).set(userData);
        console.log('User profile created in Firestore:', userRecord.uid);

        // Send verification email with clickable link
        const verificationEmailSent = await sendVerificationEmail(email, userRecord.uid);
        
        console.log('User registered successfully:', email);
        res.json({
            success: true,
            message: verificationEmailSent 
                ? 'User registered successfully. Check your email for verification link.' 
                : 'User registered but verification email failed. Try resending from app.',
            uid: userRecord.uid,
            email_verification_sent: verificationEmailSent
        });
    } catch (error) {
        console.error('User Registration Error:', error);
        res.status(500).json({ 
            error: 'Registration failed: ' + error.message,
            details: process.env.NODE_ENV === 'development' ? error.stack : undefined
        });
    }
});

// Get NGO profile
app.get('/api/ngo/profile', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'NGO') {
            return res.status(403).json({ error: 'NGO access only' });
        }

        const ngo = await db.collection('ngo_profiles').doc(req.user.uid).get();

        if (!ngo.exists) {
            return res.status(404).json({ error: 'Profile not found' });
        }

        res.json({
            success: true,
            data: { id: ngo.id, ...ngo.data() }
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ============= ADMIN APPROVAL ROUTES =============

// Approve NGO
app.post('/api/ngos/:id/approve', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'ADMIN') {
            return res.status(403).json({ error: 'Admin access required' });
        }

        const { id } = req.params;
        const { admin_notes } = req.body;

        // Get NGO details before updating
        const ngoDoc = await db.collection('ngo_profiles').doc(id).get();
        if (!ngoDoc.exists) {
            return res.status(404).json({ error: 'NGO not found' });
        }

        const ngoData = ngoDoc.data();

        await db.collection('ngo_profiles').doc(id).update({
            verification_status: 'VERIFIED',
            admin_notes: admin_notes || '',
            verified_at: admin.firestore.FieldValue.serverTimestamp()
        });

        // Send approval email to NGO
        const approvalEmailContent = `
        <h2>✓ Your NGO has been Verified!</h2>
        <p>Dear ${ngoData.organization_name},</p>
        <p>Congratulations! Your NGO has been verified and <strong>APPROVED</strong> by our admin team. Welcome to Pawsitive! 🐾</p>
        <p><strong>Organization:</strong> ${ngoData.organization_name}</p>
        <p><strong>Email:</strong> ${ngoData.ngo_email}</p>
        <p><strong>Next Steps:</strong></p>
        <ul>
            <li>Open the Pawsitive app</li>
            <li>Go to NGO Login</li>
            <li>Enter your email & password</li>
            <li>You'll be redirected to your NGO Dashboard</li>
        </ul>
        <p>${admin_notes ? '<strong>Admin Notes:</strong> ' + admin_notes + '<br><br>' : ''}You can now start using all features of the Pawsitive platform to help animals in need!</p>
        <p>Thank you for joining us!<br><strong>Best regards,<br>Pawsitive Admin Team</strong></p>
        `;
        await sendEmail(ngoData.ngo_email, '✓ Your NGO is Verified - Welcome to Pawsitive!', approvalEmailContent);

        res.json({
            success: true,
            message: 'NGO approved successfully and email sent'
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Reject NGO
app.post('/api/ngos/:id/reject', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'ADMIN') {
            return res.status(403).json({ error: 'Admin access required' });
        }

        const { id } = req.params;
        const { rejection_reason } = req.body;

        if (!rejection_reason) {
            return res.status(400).json({ error: 'Rejection reason required' });
        }

        // Get NGO details before updating
        const ngoDoc = await db.collection('ngo_profiles').doc(id).get();
        if (!ngoDoc.exists) {
            return res.status(404).json({ error: 'NGO not found' });
        }

        const ngoData = ngoDoc.data();

        await db.collection('ngo_profiles').doc(id).update({
            verification_status: 'REJECTED',
            rejection_reason,
            rejected_at: admin.firestore.FieldValue.serverTimestamp()
        });

        // Send rejection email to NGO
        const rejectionEmailContent = `
        <h2>✗ Your NGO Application Status</h2>
        <p>Dear ${ngoData.organization_name},</p>
        <p>Thank you for applying to Pawsitive. After careful review of your application, we regret to inform you that your NGO application has been <strong>REJECTED</strong> at this time.</p>
        <p><strong>Organization:</strong> ${ngoData.organization_name}</p>
        <p><strong>Reason for Rejection:</strong> ${rejection_reason}</p>
        <p>You may reapply after addressing the concerns mentioned above. If you have any questions, please contact our support team.</p>
        <p>Best regards,<br>Pawsitive Admin Team</p>
        `;
        await sendEmail(ngoData.ngo_email, '✗ NGO Application Status', rejectionEmailContent);

        res.json({
            success: true,
            message: 'NGO rejected successfully and email sent'
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ============= ADMIN STATS ROUTE =============

// Get admin stats
app.get('/api/admin/stats', verifyToken, async (req, res) => {
    try {
        if (req.user.role !== 'ADMIN') {
            return res.status(403).json({ error: 'Admin access required' });
        }

        const ngos = await db.collection('ngo_profiles').get();
        let verified = 0, pending = 0, rejected = 0;

        ngos.forEach(doc => {
            const status = doc.data().verification_status;
            if (status === 'VERIFIED') verified++;
            else if (status === 'PENDING') pending++;
            else if (status === 'REJECTED') rejected++;
        });

        const animals = await db.collection('animals').get();
        const rescues = await db.collection('rescue_reports').get();

        res.json({
            success: true,
            data: {
                totalNGOs: ngos.size,
                verifiedNGOs: verified,
                pendingNGOs: pending,
                rejectedNGOs: rejected,
                totalAnimalsPosts: animals.size,
                animalsSaved: rescues.size
            }
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
