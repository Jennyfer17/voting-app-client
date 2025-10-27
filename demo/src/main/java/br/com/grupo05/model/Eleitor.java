package br.com.grupo05.model;

import java.io.Serializable;
import java.security.SecureRandom;

public class Eleitor implements Serializable{
	private static final long serialVersionUID = 1L;

	private String id; // identificador gerado aleatoriamente
	private String fullName;

	public Eleitor() {
		this.id = generateShortId();
	}

	/**
	 * Mantive a assinatura existente (String) para compatibilidade com o cliente.
	 * Agora o parâmetro é tratado como fullName e o identificador é gerado aleatoriamente.
	 */
	public Eleitor(String fullName) {
		this.id = generateShortId();
		this.fullName = fullName;
	}

	public Eleitor(String id, String fullName) {
		this.id = (id == null || id.isEmpty()) ? generateShortId() : id;
		this.fullName = fullName;
	}

	private static String generateShortId() {
		final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		SecureRandom rnd = new SecureRandom();
		StringBuilder sb = new StringBuilder(4);
		for (int i = 0; i < 4; i++) {
			sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	// position removed from Eleitor; moved to Candidato

}
