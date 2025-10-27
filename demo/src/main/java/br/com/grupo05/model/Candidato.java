package br.com.grupo05.model;

import java.io.Serializable;

public class Candidato implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Position {
		CHEFE,
		CHEFE_ADJUNTO
	}

	private String nome;
	private int numero;
	private Position position;

	public Candidato(String nome, int numero) {
		this.nome = nome;
		this.numero = numero;
	}

	public Candidato(String nome, int numero, Position position) {
		this.nome = nome;
		this.numero = numero;
		this.position = position;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public int getNumero() {
		return numero;
	}

	public void setNumero(int numero) {
		this.numero = numero;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return nome + " " + numero + (position != null ? " (" + position.name() + ")" : "");
	}

}
