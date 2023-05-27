package com.zero.retrowrapper.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import net.minecraft.launchwrapper.LogWrapper;

public final class NetworkUtil {

    //private static final Pattern WORD = Pattern.compile("\\W");

    private static boolean isHttpResponseSuccessful(int code) {
        return (code / 100) == 2;
    }

    public static String getUUIDFromUsername(String username) {
        // TODO Would it make sense to remove invalid characters?
        //username = WORD.matcher(username).replaceAll("");
        String uuid = null;
        InputStream responseStream = null;
        InputStreamReader responseStreamReader = null;
        HttpURLConnection httpConnection = null;

        try {
            final URLConnection responseConnection = new URL("https://api.mojang.com/users/profiles/minecraft/" + username).openConnection();

            if (responseConnection instanceof HttpURLConnection) {
                httpConnection = (HttpURLConnection) responseConnection;
            }

            responseConnection.connect();

            if (httpConnection != null) {
                final int respCode = httpConnection.getResponseCode();

                if ((respCode / 100) != 2) {
                    LogWrapper.warning("Error getting UUID for username " + username + ": " + NetworkUtil.getResponseAfterErrorAndClose(httpConnection));
                    return null;
                }

                if (respCode == 204) {
                    LogWrapper.warning("Username " + username + " not found when getting UUID: " + NetworkUtil.getResponseAfterErrorAndClose(httpConnection));
                    return null;
                }
            }

            responseStream = responseConnection.getInputStream();
            responseStreamReader = new InputStreamReader(responseStream);
            final JsonObject profile = Json.parse(responseStreamReader).asObject();
            uuid = profile.get("id").asString();
        } catch (final Exception e) {
            LogWrapper.warning("Error when trying to get UUID for username " + username + ": " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(responseStreamReader);
            IOUtils.closeQuietly(responseStream);

            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }

        return uuid;
    }

    public static boolean joinServer(String sessionId, String username, String serverId) {
        return joinServerModernWithUsername(sessionId, username, serverId) || joinServerLegacy(sessionId, username, serverId);
    }

    private static boolean joinServerLegacy(String sessionId, String username, String serverId) {
        if ((sessionId == null) || (username == null) || (serverId == null)) {
            LogWrapper.warning("Can't connect to a server if a param is null!");
            return false;
        }

        InputStream responseStream = null;

        try {
            responseStream = new URL("http://session.minecraft.net/game/joinserver.jsp?user=" + username + "&sessionId=" + sessionId + "&serverId=" + serverId).openStream();
            final String response = IOUtils.toString(responseStream);
            return "ok".equalsIgnoreCase(response);
        } catch (final Exception e) {
            LogWrapper.warning("Exception thrown while trying to connect to session.minecraft.net to authenticate server connection: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(responseStream);
        }

        return false;
    }

    private static boolean joinServerModernWithUsername(String sessionId, String username, String serverId) {
        final String profileUUID = getUUIDFromUsername(username);

        if (profileUUID == null) {
            LogWrapper.warning("Could not get UUID from username " + username + ", unable to connect to server with modern authentication method.");
            return false;
        }

        return joinServerModernWithUUID(sessionId, profileUUID, serverId);
    }

    // https://wiki.vg/Protocol_Encryption#Authentication
    private static boolean joinServerModernWithUUID(String sessionId, String profileUUID, String serverId) {
        if ((sessionId == null) || (profileUUID == null) || (serverId == null)) {
            LogWrapper.warning("Can't connect to a server if a param is null!");
            return false;
        }

        boolean authenticated = false;
        final URLConnection urlConnection;
        HttpURLConnection joinSeverConnection = null;
        OutputStream askJoinStream = null;

        try {
            urlConnection = new URL("https://sessionserver.mojang.com/session/minecraft/join").openConnection();

            if (urlConnection instanceof HttpURLConnection) {
                joinSeverConnection = (HttpURLConnection) urlConnection;
                // Send a request to authenticate the user for the given server
                final JsonObject request = Json.object()
                                           .add("accessToken", sessionId)
                                           .add("selectedProfile", profileUUID)
                                           .add("serverId", serverId);
                final byte[] requestBytes = request.toString().getBytes();
                // Setup connection for sending a POST request
                joinSeverConnection.setDoOutput(true);
                joinSeverConnection.setRequestMethod("POST");
                joinSeverConnection.setRequestProperty("Content-Type", "application/json");
                joinSeverConnection.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
                askJoinStream = joinSeverConnection.getOutputStream();
                askJoinStream.write(requestBytes);
                askJoinStream.flush();
                askJoinStream.close();
                joinSeverConnection.connect();
                final int respCode = joinSeverConnection.getResponseCode();

                if (respCode != HttpURLConnection.HTTP_NO_CONTENT) {
                    if (isHttpResponseSuccessful(respCode)) {
                        // It's possible the response format might change?
                        authenticated = true;
                    }

                    // Log why we failed to authenticate
                    LogWrapper.warning("Failed to authenticate connection to server with sessionserver (HTTP code " + respCode + "):\n" + getResponseAfterErrorAndClose(joinSeverConnection));
                } else {
                    // 204 indicates the authentication was successful
                    authenticated = true;
                }
            } else {
                LogWrapper.severe("URL.openConnection() didn't return instance of HttpURLConnection");
            }
        } catch (final Exception e) {
            LogWrapper.warning("Exception thrown while trying to connect to sessionserver to authenticate server connection: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(askJoinStream);

            if (joinSeverConnection != null) {
                joinSeverConnection.disconnect();
            }
        }

        return authenticated;
    }

    public static String getBetacraftMPPass(String username, String serverIP, String serverPort) {
        String mppass = null;
        final URLConnection urlConnection;
        HttpURLConnection getMPPassUrlConnection = null;
        InputStream responseStream = null;

        try {
            urlConnection = new URL("https://api.betacraft.uk/getmppass.jsp?user=" + username + "&server=" + serverIP + ":" + serverPort).openConnection();

            if (urlConnection instanceof HttpURLConnection) {
                getMPPassUrlConnection = (HttpURLConnection) urlConnection;
                getMPPassUrlConnection.connect();
                final int respCode = getMPPassUrlConnection.getResponseCode();

                if (isHttpResponseSuccessful(respCode)) {
                    responseStream = getMPPassUrlConnection.getInputStream();
                    mppass = IOUtils.toString(responseStream);

                    if ("FAILED".equals(mppass) || "SERVER NOT FOUND".equals(mppass)) {
                        LogWrapper.warning("Failed to verify mppass with BetaCraft: " + mppass);
                        mppass = null;
                    }
                } else {
                    // Log why we failed to get the mppass
                    LogWrapper.warning("Failed to verify mppass with BetaCraft (HTTP code " + respCode + "):\n" + getResponseAfterErrorAndClose(getMPPassUrlConnection));
                }
            } else {
                LogWrapper.severe("URL.openConnection() didn't return instance of HttpURLConnection");
            }
        } catch (final Exception e) {
            LogWrapper.warning("Exception thrown while trying to get MPPass from BetaCraft: " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(responseStream);

            if (getMPPassUrlConnection != null) {
                getMPPassUrlConnection.disconnect();
            }
        }

        return mppass;
    }

    private static String getStatusLine(HttpURLConnection httpUrlConnection) {
        String httpResp;

        try {
            String statusLine = httpUrlConnection.getHeaderField(0);

            if ((statusLine == null) || (httpUrlConnection.getHeaderFieldKey(0) != null)) {
                final int statusCode = httpUrlConnection.getResponseCode();

                if (statusCode != -1) {
                    statusLine = statusCode + " " + httpUrlConnection.getResponseMessage();
                }
            }

            httpResp = statusLine == null ? "(error: connection was possibly not valid HTTP?)" : statusLine;
        } catch (final Exception ee) {
            httpResp = "(error occurred when connecting to server: " + ExceptionUtils.getStackTrace(ee) + ")";
        }

        return httpResp;
    }

    public static String getResponseAfterErrorAndClose(HttpURLConnection httpUrlConnection) {
        InputStream responseStream = null;

        try {
            responseStream = httpUrlConnection.getErrorStream();

            if (responseStream == null) {
                responseStream = httpUrlConnection.getInputStream();
            }

            final String serverRes = IOUtils.toString(responseStream);
            final StringBuilder builder = new StringBuilder();
            builder.append("HTTP status ");
            builder.append(getStatusLine(httpUrlConnection));

            if (!"".equals(serverRes.trim())) {
                builder.append(", response body: ").append(serverRes);
            } else {
                builder.append(", no response body.");
            }

            return builder.toString();
        } catch (final Exception e) {
            String url;

            try {
                url = httpUrlConnection.getURL().toExternalForm();
            } catch (final Exception ee) {
                url = "(internal error: this should never happen)";
            }

            final String httpResp = getStatusLine(httpUrlConnection);
            return "(RetroWrapper) Exception thrown when reading server response for URL " + url + " with HTTP status " + httpResp + ": " + ExceptionUtils.getStackTrace(e);
        } finally {
            IOUtils.closeQuietly(responseStream);
            httpUrlConnection.disconnect();
        }
    }

    private NetworkUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }

}
