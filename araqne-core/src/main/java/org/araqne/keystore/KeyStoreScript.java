/*
 * Copyright 2010 NCHOVY
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.keystore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Properties;

import org.araqne.api.KeyStoreManager;
import org.araqne.api.PathAutoCompleter;
import org.araqne.api.Script;
import org.araqne.api.ScriptArgument;
import org.araqne.api.ScriptContext;
import org.araqne.api.ScriptUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyStoreScript implements Script {
	private Logger logger = LoggerFactory.getLogger(KeyStoreScript.class.getName());

	private KeyStoreManager manager;
	private ScriptContext context;

	public KeyStoreScript(KeyStoreManager manager) {
		this.manager = manager;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	@ScriptUsage(description = "print certificate details in keystore", arguments = {
			@ScriptArgument(name = "name", type = "string", description = "the name of the keystore"),
			@ScriptArgument(name = "alias", type = "string", description = "the alias of the certificate") })
	public void cert(String[] args) {
		String name = args[0];
		String alias = args[1];

		KeyStore store = manager.getKeyStore(name);
		if (store == null) {
			context.println("keystore not found");
			return;
		}

		try {
			Certificate cert = store.getCertificate(alias);
			if (cert == null) {
				context.println("cert not found");
				return;
			}

			context.println(cert.toString());
		} catch (KeyStoreException e) {
			context.println("Error: " + e.getMessage());
		}
	}

	@ScriptUsage(description = "List all aliases in keystore", arguments = { @ScriptArgument(name = "alias", type = "string", description = "alias of the keystore") })
	public void aliases(String[] args) {
		String name = args[0];
		KeyStore store = manager.getKeyStore(name);
		if (store == null) {
			context.println("not found");
			return;
		}

		context.println("Aliases");
		context.println("-------------");

		try {
			Enumeration<String> it = store.aliases();
			while (it.hasMoreElements()) {
				String alias = it.nextElement();
				boolean isCertificate = store.isCertificateEntry(alias);
				boolean isKeyEntry = store.isKeyEntry(alias);
				context.printf("%s, cert=%s, key=%s\n", alias, isCertificate, isKeyEntry);
			}
		} catch (KeyStoreException e) {
			context.println("Error: " + e.getMessage());
		}
	}

	@ScriptUsage(description = "List all key stores")
	public void list(String[] args) {
		context.println("Key Stores");
		context.println("-------------");
		for (String name : manager.getKeyStoreNames()) {
			Properties p = manager.getKeyStoreProperties(name);
			String type = p.getProperty("type");
			String path = p.getProperty("path");
			context.printf("[%s] type: %s, path: %s\n", name, type, path);
		}
	}

	@ScriptUsage(description = "register keystore", arguments = {
			@ScriptArgument(name = "name", description = "the name of the key store"),
			@ScriptArgument(name = "type", description = "the type of the key store. for example, JKS, PKCS12, etc."),
			@ScriptArgument(name = "file path", description = "the file path of the key store", autocompletion = PathAutoCompleter.class)})
	public void register(String[] args) {
		try {
			String alias = args[0];
			String type = args[1];
			String path = args[2];
			context.print("Key-Store Password? ");
			char[] password = context.readPassword().toCharArray();
			
			manager.registerKeyStore(alias, type, path, password);
			context.printf("[%s] key store registered\n", alias);
		} catch (KeyStoreException e) {
			context.printf("key store exception: %s", e.getMessage());
			logger.warn("keystore.register: ", e);
		} catch (FileNotFoundException e) {
			context.printf("file not found: %s\n", e.getMessage());
			logger.warn("keystore.register: ", e);
		} catch (NoSuchAlgorithmException e) {
			context.printf("no such algorithm: %s\n", e.getMessage());
			logger.warn("keystore.register: ", e);
		} catch (CertificateException e) {
			context.printf("certificate error: %s\n", e.getMessage());
			logger.warn("keystore.register: ", e);
		} catch (IOException e) {
			context.printf("io exception: %s\n", e.getMessage());
			logger.warn("keystore.register: ", e);
		} catch (InterruptedException e) {
			context.println("interrupted");
		}
	}

	@ScriptUsage(description = "unregister keystore", arguments = { @ScriptArgument(name = "name", description = "alias of the key store") })
	public void unregister(String[] args) {
		String name = args[0];
		manager.unregisterKeyStore(name);
		context.printf("[%s] key store unregistered\n", name);
	}

}
