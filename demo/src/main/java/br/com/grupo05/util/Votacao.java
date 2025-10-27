package br.com.grupo05.util;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import br.com.grupo05.model.Candidato;
import br.com.grupo05.model.Eleitor;

public interface Votacao extends Remote{

	public List<Candidato> getCandidatos() throws RemoteException ;
	public boolean setVoto(Eleitor eleitor, Candidato candidato) throws RemoteException;
	public String getResultadoVotacao() throws RemoteException;
	public boolean isEleitor(Eleitor eleitor) throws RemoteException;

	// Novos métodos RMI expostos
	public boolean registrarCandidato(Candidato candidato) throws RemoteException;
	public int registrarEleitor(Eleitor eleitor) throws RemoteException; // retorna id do eleitor no BD
	public boolean atualizarVoto(Eleitor eleitor, Candidato candidato) throws RemoteException;
	public boolean isVotandoSegundaVez(Eleitor eleitor) throws RemoteException;
	public boolean isVotandoEmSiMesmo(Eleitor eleitor, Candidato candidato) throws RemoteException;
	public String obterIdentificadorEleitor(Eleitor eleitor) throws RemoteException; // id curto/identificador
	public int obterIdEleitorDb(Eleitor eleitor) throws RemoteException; // id numérico no BD
	public Map<Integer, Integer> getVotosPorCandidato() throws RemoteException;
	
	/* Time control methods have been removed */
}
