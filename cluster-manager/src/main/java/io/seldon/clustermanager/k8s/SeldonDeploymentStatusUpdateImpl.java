/*******************************************************************************
 * Copyright 2017 Seldon Technologies Ltd (http://www.seldon.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package io.seldon.clustermanager.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.seldon.protos.DeploymentProtos.PredictorStatus;
import io.seldon.protos.DeploymentProtos.SeldonDeployment;

@Component
public class SeldonDeploymentStatusUpdateImpl implements SeldonDeploymentStatusUpdate {
    protected static Logger logger = LoggerFactory.getLogger(SeldonDeploymentStatusUpdateImpl.class.getName());
	private final KubeCRDHandler crdHandler;
	
	@Autowired
	public SeldonDeploymentStatusUpdateImpl(KubeCRDHandler crdHandler) {
		this.crdHandler = crdHandler;
	}

	private void update(PredictorStatus.Builder b,Integer replicas, Integer replicasAvailable)
	{
	    if (replicas != null)
	        b.setReplicas(replicas);
	    else
	        b.setReplicas(0);
	    if (replicasAvailable != null)
	        b.setReplicasAvailable(replicasAvailable);
	    else
	        b.setReplicasAvailable(0);
	}
	
    @Override
    public void updateStatus(String mlDepName, String depName, Integer replicas, Integer replicasAvailable) {
        if (replicas == null || replicas == 0)
            removeStatus(mlDepName,depName);
        else
        {
            logger.info(String.format("UPDATE %s : %s %d %d",mlDepName,depName,replicas,replicasAvailable));
            SeldonDeployment mlDep = crdHandler.getSeldonDeployment(mlDepName);
            if (mlDep != null)
            {
                SeldonDeployment.Builder mlBuilder = SeldonDeployment.newBuilder(mlDep);
                
               boolean changed = false;
               for (PredictorStatus.Builder b : mlBuilder.getStatusBuilder().getPredictorStatusBuilderList())
               {
                  if (b.getName().equals(depName))
                  {
                      update(b,replicas,replicasAvailable);
                      changed = true;
                      break;
                  }
               }
               if (!changed)
               {
                   PredictorStatus.Builder b = PredictorStatus.newBuilder().setName(depName);
                   update(b,replicas,replicasAvailable);
                   mlBuilder.getStatusBuilder().addPredictorStatus(b);
               }
               crdHandler.updateSeldonDeployment(mlBuilder.build());
            }
            else
                logger.error("Can't find seldondeployment "+mlDepName+" to update "+depName);
        }
        
    }

    @Override
    public void removeStatus(String mlDepName, String depName) {
        logger.info(String.format("DELETE %s : %s",mlDepName,depName));
        SeldonDeployment mlDep = crdHandler.getSeldonDeployment(mlDepName);
        if (mlDep != null)
        {
            SeldonDeployment.Builder mlBuilder = SeldonDeployment.newBuilder(mlDep);
            int idx = 0;
            for (PredictorStatus.Builder b : mlBuilder.getStatusBuilder().getPredictorStatusBuilderList())
            {
                if (b.getName().equals(depName))
                {
                    mlBuilder.getStatusBuilder().removePredictorStatus(idx);
                    break;
                }
            }
            crdHandler.updateSeldonDeployment(mlBuilder.build());
        }
        else
            logger.error("Can't find seldondeployment "+mlDepName+" to remove "+depName);
    }
	
}
