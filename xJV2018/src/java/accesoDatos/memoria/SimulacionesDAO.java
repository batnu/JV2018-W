/** 
 * Proyecto: Juego de la vida.
 *  Resuelve todos los aspectos del almacenamiento del DTO Simulacion utilizando un ArrayList.
 *  Colabora en el patron Fachada.
 *  @since: prototipo2.0
 *  @source: SimulacionesDAO.java 
 *  @version: 2.0 - 2019/03/20 
 *  @author: ajp
 */

package accesoDatos.memoria;

import java.util.ArrayList;
import java.util.List;

import accesoDatos.DatosException;
import accesoDatos.OperacionesDAO;
import modelo.ModeloException;
import modelo.Mundo;
import modelo.SesionUsuario;
import modelo.Simulacion;
import modelo.Simulacion.EstadoSimulacion;
import modelo.Usuario;
import util.Fecha;

public class SimulacionesDAO implements OperacionesDAO {

	// Singleton 
	private static SimulacionesDAO instancia;

	// Elemento de almacenamiento.
	private ArrayList<Simulacion> datosSimulaciones;

	/**
	 * Constructor por defecto de uso interno.
	 * Sólo se ejecutará una vez.
	 */
	private SimulacionesDAO() {
		datosSimulaciones = new ArrayList<Simulacion>();
		cargarPredeterminados();
	}

	/**
	 *  Método estático de acceso a la instancia única.
	 *  Si no existe la crea invocando al constructor interno.
	 *  Utiliza inicialización diferida.
	 *  Sólo se crea una vez; instancia única -patrón singleton-
	 *  @return instancia
	 * @throws DatosException 
	 */
	public static SimulacionesDAO getInstancia() throws DatosException {
		if (instancia == null) {
			instancia = new SimulacionesDAO();
		}
		return instancia;
	}

	/**
	 *  Método para generar datos predeterminados. 
	 */
	private void cargarPredeterminados() {
		try {
			// Obtiene usuario (invitado) y mundo predeterminados.
			Usuario usrDemo = UsuariosDAO.getInstancia().obtener("III1R");
			Mundo mundoDemo = MundosDAO.getInstancia().obtener("Demo1");
			alta(new Simulacion(usrDemo, new Fecha(0001, 01, 01, 01, 01, 01), mundoDemo, EstadoSimulacion.PREPARADA));
		} 
		catch (DatosException e) {
			e.printStackTrace();
		}
	}

	// OPERACIONES DAO
	/**
	 * Búsqueda de Simulacion dado idUsr y fecha.
	 * @param id - el idUsr+fecha de la Simulacion a buscar. 
	 * @return - la Simulacion encontrada; null si no encuentra. 
	 */	
	@Override
	public Simulacion obtener(String id) {
		assert id != null;
		int posicion = indexSort(id);						// En base 1
		if (posicion >= 0) {
			return datosSimulaciones.get(posicion - 1);     // En base 0
		}
		return null;
	}

	/**
	 *  Obtiene por búsqueda binaria, la posición que ocupa, o ocuparía,  una simulación en 
	 *  la estructura.
	 *	@param id - id de Simulacion a buscar.
	 *	@return - la posición, en base 1, que ocupa un objeto o la que ocuparía (negativo).
	 */
	private int indexSort(String id) {
		int comparacion;
		int inicio = 0;
		int fin = datosSimulaciones.size() - 1;
		int medio = 0;
		while (inicio <= fin) {
			medio = (inicio + fin) / 2;			// Calcula posición central.
			// Obtiene > 0 si idSimulacion va después que medio.
			comparacion = id.compareTo(datosSimulaciones.get(medio).getId());
			if (comparacion == 0) {			
				return medio + 1;   			// Posción ocupada, base 1	  
			}		
			if (comparacion > 0) {
				inicio = medio + 1;
			}			
			else {
				fin = medio - 1;
			}
		}	
		return -(inicio + 1);					// Posición que ocuparía -negativo- base 1
	}

	/**
	 * obtiene todas las simulaciones en una lista.
	 * @return - la lista.
	 */
	@Override
	public List obtenerTodos() {
		return datosSimulaciones;
	}

	/**
	 * Búsqueda de todas la simulaciones de un usuario.
	 * @param idUsr - el identificador de usuario a buscar.
	 * @return - Sublista con las simulaciones encontrada; null si no existe ninguna.
	 * @throws ModeloException 
	 */
	public List<Simulacion> obtenerTodasMismoUsr(String idUsr) throws ModeloException {
		Simulacion aux = null;
		aux = new Simulacion();
		aux.setUsr(UsuariosDAO.getInstancia().obtener(idUsr));
		//Busca posición inserción ordenada por idUsr + fecha. La última para el mismo usuario.
		return separarSimulacionesUsr(indexSort(aux.getId()) - 1);
	}

	/**
	 * Separa en una lista independiente todas la simulaciones de un mismo usuario.
	 * @param ultima - el indice de la última simulación ya encontrada.
	 * @return - Sublista con las simulaciones encontrada; null si no existe ninguna.
	 */
	private List<Simulacion> separarSimulacionesUsr(int ultima) {
		// Localiza primera simulación del mismo usuario.
		String idUsr = datosSimulaciones.get(ultima).getUsr().getId();
		int primera = ultima;
		for (int i = ultima; i >= 0 && datosSimulaciones.get(i).getUsr().getId().equals(idUsr); i--) {
			primera = i;
		}
		// devuelve la sublista de simulaciones buscadas.
		return datosSimulaciones.subList(primera, ultima+1);
	}

	/**
	 *  Alta de una nueva Simulacion en orden y sin repeticiones según los idUsr más fecha. 
	 *  Busca previamente la posición que le corresponde por búsqueda binaria.
	 *  @param obj - Simulación a almacenar.
	 * @throws DatosException - si ya existe.
	 */	
	public void alta(Object obj) throws DatosException  {
		assert obj != null;
		Simulacion simulacion = (Simulacion) obj;								// Para conversión cast
		int posInsercion = indexSort(simulacion.getId()); 
		if (posInsercion < 0) {
			datosSimulaciones.add(Math.abs(posInsercion)-1, simulacion); 		// Inserta la simulación en orden.
		}
		else {
			throw new DatosException("SimulacionesDAO.alta: "+ simulacion.getId() + " ya existe");
		}
	}

	/**
	 * Elimina el objeto, dado el id utilizado para el almacenamiento.
	 * @param idSimulacion - identificador de la Simulacion a eliminar.
	 * @return - la Simulacion eliminada. 
	 * @throws DatosException - si no existe.
	 */
	@Override
	public Simulacion baja(String idSimulacion) throws DatosException  {
		assert (idSimulacion != null);
		int posicion = indexSort(idSimulacion); 								// En base 1
		if (posicion > 0) {
			return datosSimulaciones.remove(posicion - 1); 						// En base 0
		}
		else {
			throw new DatosException("SimulacionesDAO.baja: "+ idSimulacion + " no existe");
		}
	}
	
	/**
	 *  Actualiza datos de una Simulacion reemplazando el almacenado por el recibido.
	 *  No admitirá cambios en usr ni en la fecha.
	 *	@param obj - Patron con las modificaciones.
	 * @throws DatosException - si no existe.
	 */
	@Override
	public void actualizar(Object obj) throws DatosException  {
		assert obj != null;
		Simulacion simulActualizada = (Simulacion) obj;							// Para conversión cast
		int posicion = indexSort(simulActualizada.getId()); 					// En base 1
		if (posicion > 0) {
			// Reemplaza elemento
			datosSimulaciones.set(posicion - 1, simulActualizada);  			// En base 0		
		}
		else {
			throw new DatosException("SimulacionesDAO.actualizar: "+ simulActualizada.getId() + "no existe");
		}
	}

	/**
	 * Obtiene el listado de todos las simulaciones almacenadas.
	 * @return el texto con el volcado de datos.
	 */
	@Override
	public String listarDatos() {
		StringBuilder result = new StringBuilder();
		for (Simulacion simulacion: datosSimulaciones) {
			result.append("\n" + simulacion);
		}
		return result.toString();
	}

	/**
	 * Obtiene el listado de todos id de los objetos almacenados.
	 * @return el texto con el volcado de id.
	 */
	@Override
	public String listarId() {
		StringBuilder result = new StringBuilder();
		for (Simulacion simulacion: datosSimulaciones) {
			result.append("\n" + simulacion.getId()); 
		}
		return result.toString();
	}

	/**
	 * Elimina todos las simulaciones almacenadas y regenera la demo predeterminada.
	 */
	@Override
	public void borrarTodo() {
		datosSimulaciones.clear();
		cargarPredeterminados();
	}

} //class
