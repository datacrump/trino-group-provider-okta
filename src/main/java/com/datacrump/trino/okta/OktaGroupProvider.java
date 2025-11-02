package com.datacrump.trino.okta;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.okta.sdk.client.AuthorizationMode;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.api.UserResourcesApi;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.model.Group;
import com.okta.sdk.resource.model.GroupProfile;

import java.io.IOException;
import io.trino.spi.security.GroupProvider;



public class OktaGroupProvider implements GroupProvider {
    private static final String PKCS_8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS_8_FOOTER = "-----END PRIVATE KEY-----";
    private static final String RSA_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String RSA_FOOTER = "-----END RSA PRIVATE KEY-----";

    private static final Logger log = LoggerFactory.getLogger(OktaGroupProvider.class);

    private final ApiClient client;
    private final Pattern groupPattern;

    public OktaGroupProvider(String oktaDomain, String clientId, List<String> scopes, String privateKeyPath, String groupPatternString) {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPath);    
            this.client = Clients.builder()
            .setOrgUrl("https://" + oktaDomain)  // e.g. https://dev-123456.okta.com
            .setClientId(clientId)
            .setAuthorizationMode(AuthorizationMode.PRIVATE_KEY)
            .setPrivateKey(privateKey)
            .setScopes(new HashSet<>(scopes))
            .build();
            System.out.println(scopes);
            this.groupPattern = Pattern.compile(groupPatternString, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            log.error("Error loading private key from path {}", privateKeyPath, e);
            throw new RuntimeException("Error loading private key from path " + privateKeyPath, e);
        }
        
        
        
    }

    @Override
    public Set<String> getGroups(String user) {
        try {
            Set<String> groups = new HashSet<>();
            
            UserResourcesApi userResourcesApi = new UserResourcesApi(client);

            List<Group> oktaUserGroups = userResourcesApi.listUserGroups(user);
            for (Group oktaGroup : oktaUserGroups) {
                String groupName = getGroupName(oktaGroup);
                if (groupName != null) {
                    groups.add(groupName);
                }
            }
            return groups;
        } catch (RuntimeException e) {
            System.out.println("Error retrieving groups for user " + user + " from Okta: " + e.getMessage());
            System.out.println("Error stack trace: " + e.getStackTrace());  
            e.printStackTrace();
            log.error("Error retrieving groups for user {} from Okta", user, e);
            return Set.of();
        }
    }

    /**
     * Get group name by extracting name from group profile using the group pattern.
     * 
     * If the pattern contains capturing groups (e.g., `trino_group_(.*)`), the captured
     * portion will be used as the group name. Otherwise, the full matched group name is returned.
     * 
     * Examples:
     * - Pattern `.*`: matches all groups, returns full group name as-is
     * - Pattern `trino_group_(.*)`: matches groups starting with `trino_group_`, 
     *   returns only the part after the prefix (e.g., `trino_group_developers` -> `developers`)
     * 
     * @param group The Okta group object
     * @return The extracted group name, or null if the group doesn't match the pattern or profile is null
     */
    public String getGroupName(Group group) {
        if (group == null) {
            return null;
        }
        
        GroupProfile groupProfile = group.getProfile();
        if (groupProfile == null) {
            return null;
        }
        
        String oktaGroupName = groupProfile.getName();
        if (oktaGroupName == null) {
            return null;
        }
        
        Matcher matcher = groupPattern.matcher(oktaGroupName);
        
        if (matcher.matches()) {
            // Check if pattern has capturing groups
            int groupCount = matcher.groupCount();
            if (groupCount > 0) {
                // Pattern has capturing groups - extract the first captured group
                // For pattern like "trino_group_(.*)", this will return the part after "trino_group_"
                String extractedName = matcher.group(1);
                if (extractedName != null && !extractedName.isEmpty()) {
                    return extractedName;
                }
            }
            // No capturing groups or default pattern (.*) - return full group name
            return oktaGroupName;
        }
        
        return null;
    }


/**
     * Loads a PrivateKey object from a PEM-encoded key file.
     * This function manually strips PEM headers and decodes the Base64 content.
     *
     * @param filename The path to the private key file (e.g., "path/to/private.key").
     * @return A valid PrivateKey object.
     * @throws IOException If the file cannot be read.
     * @throws InvalidKeySpecException If the key specification is invalid.
     * @throws NoSuchAlgorithmException If the required cryptographic algorithm is not found.
     */
    private PrivateKey loadPrivateKey(String filename) 
        throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        
        // 1. Read the entire file content
        String keyContent = new String(Files.readAllBytes(Paths.get(filename)));

        // 2. Clean the string: remove common headers, footers, and all whitespace
        String cleanKey = keyContent
            .replace(PKCS_8_HEADER, "")
            .replace(PKCS_8_FOOTER, "")
            .replace(RSA_HEADER, "")
            .replace(RSA_FOOTER, "")
            .replaceAll("\\s", ""); // Remove all whitespace (spaces, newlines, etc.)

        // 3. Decode the Base64 content to get the raw DER bytes
        byte[] keyBytes = Base64.getDecoder().decode(cleanKey);

        // 4. Create the PKCS8EncodedKeySpec from the DER bytes
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        // 5. Generate the PrivateKey using KeyFactory
        // We try RSA first, which covers many keys, but the algorithm might vary
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            // Fallback for non-RSA keys if a generic provider is configured
            KeyFactory kf = KeyFactory.getInstance("JCE"); 
            return kf.generatePrivate(keySpec);
        }
    }
}

