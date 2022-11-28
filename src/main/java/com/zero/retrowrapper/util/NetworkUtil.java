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
import com.eclipsesource.json.PrettyPrint;

import net.minecraft.launchwrapper.LogWrapper;

public final class NetworkUtil {

    //private static final Pattern WORD = Pattern.compile("\\W");

    public static boolean isHttpResponseSuccessful(int code) {
        return (code / 100) == 2;
    }

    public static String getUUIDFromUsername(String username) {
        // TODO Would it make sense to remove invalid characters?
        //username = WORD.matcher(username).replaceAll("");
        String uuid = null;
        InputStream is = null;
        InputStreamReader reader = null;

        try {
            is = new URL("https://api.mojang.com/users/profiles/minecraft/" + username + "?at=" + System.currentTimeMillis()).openStream();
            reader = new InputStreamReader(is);
            final JsonObject profile1 = (JsonObject) Json.parse(reader);
            uuid = profile1.get("id").asString();
        } catch (final Exception e) {
            LogWrapper.warning("Error when trying to get UUID for username " + username + ": " + ExceptionUtils.getStackTrace(e));
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(is);
        }

        return uuid;
    }

    public static boolean joinServer(String sessionId, String username, String serverId) {
        final String profileUUID = NetworkUtil.getUUIDFromUsername(username);
        return joinServerModern(sessionId, profileUUID, serverId) || joinServerLegacy(sessionId, username, serverId);
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

    // https://wiki.vg/Protocol_Encryption#Authentication
    private static boolean joinServerModern(String sessionId, String profileUUID, String serverId) {
        if ((sessionId == null) || (profileUUID == null) || (serverId == null)) {
            LogWrapper.warning("Can't connect to a server if a param is null!");
            return false;
        }

        boolean authenticated = false;
        URLConnection urlConnection;
        HttpURLConnection joinSeverConnection = null;
        OutputStream askJoinStream = null;
        InputStream responseStream = null;

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

                    // Check why we failed to authenticate
                    responseStream = joinSeverConnection.getErrorStream();

                    if (responseStream == null) {
                        responseStream = joinSeverConnection.getInputStream();
                    }

                    String failedResponseMessage = "(no response from server)";

                    try {
                        failedResponseMessage = IOUtils.toString(responseStream);
                        failedResponseMessage = Json.parse(failedResponseMessage).toString(PrettyPrint.indentWithSpaces(4));
                    } catch (final Exception e) {
                        // This should always be JSON, but it's possible that it'll be changed in the future
                        LogWrapper.warning("Response wasn't JSON?: " + ExceptionUtils.getStackTrace(e));
                    }

                    LogWrapper.warning("Failed to authenticate connection to server with sessionserver (HTTP code " + respCode + "):\n" + failedResponseMessage);
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
            IOUtils.closeQuietly(responseStream);

            if (joinSeverConnection != null) {
                joinSeverConnection.disconnect();
            }
        }

        return authenticated;
    }

    public static String getBetacraftMPPass(String username, String serverIP, String serverPort) {
        String mppass = null;
        URLConnection urlConnection;
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
                    // Check why we failed to get the mppass
                    responseStream = getMPPassUrlConnection.getErrorStream();

                    if (responseStream == null) {
                        responseStream = getMPPassUrlConnection.getInputStream();
                    }

                    String failedResponseMessage;

                    try {
                        failedResponseMessage = IOUtils.toString(responseStream);
                    } catch (final Exception e) {
                        LogWrapper.warning("Issue reading mppass response: " + ExceptionUtils.getStackTrace(e));
                        failedResponseMessage = "(no response from server)";
                    }

                    LogWrapper.warning("Failed to verify mppass with BetaCraft (HTTP code " + respCode + "):\n" + failedResponseMessage);
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

    private NetworkUtil() {
        // As this is a helper class, there should be no reason to instantiate an instance of it.
    }

}
