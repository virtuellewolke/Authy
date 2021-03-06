import Vue from 'vue'
import Router from 'vue-router'
import Login from '../views/Login'
import Profile from "../views/Profile";
import Logout from "../views/Logout";
import AdminUsers from "../views/AdminUsers";
import AdminUsersCreate from "../views/AdminUsersCreate";
import AdminUsersModify from "../views/AdminUsersModify";
import ProfileSecurity from "../views/ProfileSecurity";
import AdminAuthority from "../views/AdminAuthority";
import AdminServices from "../views/AdminServices";
import AdminServicesCreate from "../views/AdminServicesCreate";
import AdminServicesModify from "../views/AdminServicesModify";
import ErrorPage from "../views/ErrorPage";

Vue.use(Router);

let router = new Router({
    routes: [
        {
            path: '/',
            name: 'Profile',
            component: Profile,
            meta: {
                authenticated: true,
            }
        },
        {
            path: '/error',
            name: 'ErrorPage',
            component: ErrorPage,
        },
        {
            path: '/profile/security',
            name: 'ProfileSecurity',
            component: ProfileSecurity,
            meta: {
                authenticated: true,
            }
        },
        {
            path: '/login',
            name: 'Login',
            component: Login,
            props: {cas: false},
            meta: {
                unauthenticated: true
            }
        },
        {
            path: '/cas/login',
            name: 'CasLogin',
            component: Login,
            props: {cas: true},
            meta: {
                unauthenticated: true
            }
        },
        {
            path: '/logout',
            name: 'Logout',
            component: Logout,
            meta: {
                authenticated: true
            }
        },
        {
            path: '/admin/users',
            component: AdminUsers,
            meta: {
                admin: true
            }
        },
        {
            path: '/admin/users/create',
            component: AdminUsersCreate,
            meta: {
                admin: true
            }
        },
        {
            path: '/admin/user/:id',
            component: AdminUsersModify,
            meta: {
                admin: true
            }
        },
        {
            path: '/admin/authorities',
            component: AdminAuthority,
            meta: {
                admin: true
            }
        },
        {
            path: '/admin/services',
            component: AdminServices,
            meta: {
                admin: true
            }
        },
        {
            path: '/admin/services/create',
            component: AdminServicesCreate,
            meta: {
                admin: true
            }
        },
        {
            path: '/admin/service/:id',
            component: AdminServicesModify,
            meta: {
                admin: true
            }
        },
    ]
});

router.beforeEach((to, from, next) => {
    let requiresAuth = to.matched.some(r => r.meta.authenticated);
    let requiresAdmin = to.matched.some(r => r.meta.admin);
    let requiresAnon = to.matched.some(r => r.meta.unauthenticated);
    let isSignedIn = Vue.prototype.$store.state.authenticationToken !== "";
    let isAdmin = isSignedIn && Vue.prototype.$store.state.admin;

    if ((requiresAdmin || requiresAuth) && !isSignedIn) { // AUTH REQUIRED
        console.log("[ROUTER] Authentication is required. (path=" + to.path + ")");
        next({path: '/login', query: {service: '/'}})
    } else if (requiresAdmin && !isAdmin) {// ADMIN REQUIRED
        console.log("[ROUTER] Access Denied. (path=" + to.path + ")");
        next(from);
    } else if (requiresAnon && isSignedIn) {
        console.log("[ROUTER] Authentication is not allowed here. (path=" + to.path + ")");
        next(from);
    } else {
        next();
    }
});

export default router;