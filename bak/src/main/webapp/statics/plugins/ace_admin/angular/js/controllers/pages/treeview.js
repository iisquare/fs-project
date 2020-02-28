angular.module('AceApp').controller('TreeviewCtrl', ['$scope', function($scope) {
	
	function createSubTree(level, width, prefix) {
        if (level > 0) {
            var res = [];
            for (var i=1; i <= width; i++)
                res.push({ "label" : "Node " + prefix + i, "id" : "id"+prefix + i, "i": i, "children": createSubTree(level-1, width, prefix + i +".") });
            return res;
        }
        else
            return [];
    };
	
	$scope.treeOptions = {
		injectClasses : {
			ul: 'tree tree-branch-children',
			li: 'tree-branch',
			liSelected: 'tree-selected',
			iExpanded: 'icon-caret ace-icon tree-minus',
			iCollapsed: 'icon-caret ace-icon tree-plus',
			iLeaf: 'icon-item ace-icon fa fa-circle bigger-110',
			label: 'tree-branch-header'
		}
    };
	
	$scope.treeData = createSubTree(2, 4, "");

}]);
