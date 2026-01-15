import { Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import {
  Menu,
  X,
  GitBranch,
  Search,
  LogIn,
  LogOut,
  User,
  Bubbles,
  MessageCircle,
} from "lucide-react";
import { useAuth } from "react-oidc-context";
import { newThreadLink } from "@/lib/utils";

export default function Header() {
  const [isOpen, setIsOpen] = useState(false);
  const auth = useAuth();
  const navigate = useNavigate();

  return (
    <>
      <header className="flex justify-between items-center p-4 text-white bg-gray-800 shadow-lg">
        <div className="flex items-center">
          <button
            onClick={() => setIsOpen(true)}
            className="p-2 rounded-lg transition-colors hover:bg-gray-700"
            aria-label="Open menu"
          >
            <Menu size={24} />
          </button>
          <h1 className="ml-4 text-xl font-semibold">
            <Link to="/">
              <img
                src="/tanstack-word-logo-white.svg"
                alt="TanStack Logo"
                className="h-10"
              />
            </Link>
          </h1>
        </div>

        <div className="flex gap-4 items-center">
          {auth.isAuthenticated ? (
            <>
              <div className="flex gap-2 items-center">
                <User size={20} />
                <span className="text-sm">
                  {auth.user?.profile.preferred_username ||
                    auth.user?.profile.email}
                </span>
              </div>
              <button
                onClick={() => auth.removeUser()}
                className="flex gap-2 items-center py-2 px-4 bg-red-600 rounded-lg transition-colors hover:bg-red-700"
              >
                <LogOut size={20} />
                <span>Logout</span>
              </button>
            </>
          ) : (
            <button
              onClick={() => auth.signinRedirect()}
              className="flex gap-2 items-center py-2 px-4 bg-cyan-600 rounded-lg transition-colors hover:bg-cyan-700"
              disabled={auth.isLoading}
            >
              <LogIn size={20} />
              <span>{auth.isLoading ? "Loading..." : "Login"}</span>
            </button>
          )}
        </div>
      </header>

      <aside
        className={`fixed top-0 left-0 h-full w-80 bg-gray-900 text-white shadow-2xl z-50 transform transition-transform duration-300 ease-in-out flex flex-col ${
          isOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex justify-between items-center p-4 border-b border-gray-700">
          <h2 className="text-xl font-bold">Navigation</h2>
          <button
            onClick={() => setIsOpen(false)}
            className="p-2 rounded-lg transition-colors hover:bg-gray-800"
            aria-label="Close menu"
          >
            <X size={24} />
          </button>
        </div>

        <nav className="overflow-y-auto flex-1 p-4">
          <Link
            to="/repositories"
            onClick={() => setIsOpen(false)}
            className="flex gap-3 items-center p-3 mb-2 rounded-lg transition-colors hover:bg-gray-800"
            activeProps={{
              className:
                "flex items-center gap-3 p-3 rounded-lg bg-cyan-600 hover:bg-cyan-700 transition-colors mb-2",
            }}
          >
            <GitBranch size={20} />
            <span className="font-medium">Repositories</span>
          </Link>

          <Link
            to="/search"
            onClick={() => setIsOpen(false)}
            className="flex gap-3 items-center p-3 mb-2 rounded-lg transition-colors hover:bg-gray-800"
            activeProps={{
              className:
                "flex items-center gap-3 p-3 rounded-lg bg-cyan-600 hover:bg-cyan-700 transition-colors mb-2",
            }}
          >
            <Search size={20} />
            <span className="font-medium">Code Search</span>
          </Link>
          <button
            onClick={() => {
              setIsOpen(false);
              navigate({ to: newThreadLink('chat', 'ghibliAgent') as any });
            }}
            className="flex gap-3 items-center p-3 mb-2 rounded-lg transition-colors hover:bg-gray-800 w-full text-left"
          >
            <MessageCircle size={20} />
            <span className="font-medium">Chat</span>
          </button>
        </nav>
      </aside>
    </>
  );
}
